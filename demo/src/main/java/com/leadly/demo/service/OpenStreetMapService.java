package com.leadly.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadly.demo.dto.LeadSearchResultDTO;
import com.leadly.demo.dto.OverpassElement;
import com.leadly.demo.dto.OverpassResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class OpenStreetMapService {

    private static final Logger log = LoggerFactory.getLogger(OpenStreetMapService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9 à-úÀ-Ú\\-_]");

    // Servidores Overpass públicos (Failover Strategy)
    private static final List<String> OVERPASS_ENDPOINTS = List.of(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
    );

    // =========================================================
    // PARÂMETROS DE CUSTO DA QUERY OVERPASS
    // =========================================================
    // Reduzido de 10km -> 7km: a área de busca cai pela metade (πr²),
    // o que reduz drasticamente a quantidade de elementos candidatos
    // em cidades grandes e densas como Salvador, evitando 504/500
    // por sobrecarga nos servidores públicos.
    private static final int RAIO_BUSCA_METROS = 7000;

    // Timeout interno da query no servidor Overpass (parâmetro [timeout:N] da query QL).
    private static final int OVERPASS_TIMEOUT_SEGUNDOS = 30;

    // Limite de elementos retornados pelo "out". Cidades grandes podem ter
    // milhares de POIs num raio de 7km; sem limite, a serialização da resposta
    // (e o tempo de rede) cresce sem necessidade real para uma lista de leads.
    private static final int MAX_RESULTADOS_OVERPASS = 200;

    // Timeouts do cliente HTTP (evita requisição travada por minutos quando
    // o servidor Overpass não responde nem com erro).
    private static final Duration OVERPASS_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration OVERPASS_READ_TIMEOUT = Duration.ofSeconds(35);

    // Limiar mínimo de similaridade para aceitar correspondência aproximada de categoria
    // (ex.: "dentisa" -> "dentista"). Abaixo disso, não arriscamos falso positivo.
    private static final double SIMILARIDADE_MINIMA_CATEGORIA = 0.80;
    private static final int TAMANHO_MINIMO_PALAVRA_FUZZY = 4;

    // Tabela de Estados do Brasil (Sigla -> Nome Oficial)
    private static final Map<String, String> ESTADOS_BR = new HashMap<>();
    static {
        ESTADOS_BR.put("AC", "Acre");
        ESTADOS_BR.put("AL", "Alagoas");
        ESTADOS_BR.put("AP", "Amapá"); // Corrigido bug Aapá -> Amapá
        ESTADOS_BR.put("AM", "Amazonas");
        ESTADOS_BR.put("BA", "Bahia");
        ESTADOS_BR.put("CE", "Ceará");
        ESTADOS_BR.put("DF", "Distrito Federal");
        ESTADOS_BR.put("ES", "Espírito Santo");
        ESTADOS_BR.put("GO", "Goiás");
        ESTADOS_BR.put("MA", "Maranhão");
        ESTADOS_BR.put("MT", "Mato Grosso");
        ESTADOS_BR.put("MS", "Mato Grosso do Sul");
        ESTADOS_BR.put("MG", "Minas Gerais");
        ESTADOS_BR.put("PA", "Pará");
        ESTADOS_BR.put("PB", "Paraíba");
        ESTADOS_BR.put("PR", "Paraná");
        ESTADOS_BR.put("PE", "Pernambuco");
        ESTADOS_BR.put("PI", "Piauí");
        ESTADOS_BR.put("RJ", "Rio de Janeiro");
        ESTADOS_BR.put("RN", "Rio Grande do Norte");
        ESTADOS_BR.put("RS", "Rio Grande do Sul");
        ESTADOS_BR.put("RO", "Rondônia");
        ESTADOS_BR.put("RR", "Roraima");
        ESTADOS_BR.put("SC", "Santa Catarina");
        ESTADOS_BR.put("SP", "São Paulo");
        ESTADOS_BR.put("SE", "Sergipe");
        ESTADOS_BR.put("TO", "Tocantins");
    }

    // =========================================================
    // MAPA DE CATEGORIAS -> TAGS OSM
    // =========================================================
    // Estruturado como palavras-chave normalizadas -> tags OSM.
    // Mantém exatamente o mesmo mapeamento de tags do arquivo original;
    // apenas foi reorganizado para permitir reuso no matching aproximado (fuzzy).
    private static final Map<List<String>, List<String>> MAPA_CATEGORIAS = new LinkedHashMap<>();
    static {
        MAPA_CATEGORIAS.put(List.of("dentista", "odonto"),
                List.of("\"amenity\"=\"dentist\"", "\"healthcare\"=\"dentist\""));
        MAPA_CATEGORIAS.put(List.of("restaurante", "pizzaria", "churrascaria"),
                List.of("\"amenity\"=\"restaurant\""));
        MAPA_CATEGORIAS.put(List.of("farmacia", "drogaria"),
                List.of("\"amenity\"=\"pharmacy\""));
        MAPA_CATEGORIAS.put(List.of("hospital", "pronto socorro"),
                List.of("\"amenity\"=\"hospital\""));
        MAPA_CATEGORIAS.put(List.of("cafe", "lanchonete"),
                List.of("\"amenity\"=\"cafe\""));
        MAPA_CATEGORIAS.put(List.of("medico", "clinica", "consultorio"),
                List.of("\"amenity\"=\"clinic\"", "\"healthcare\"=\"clinic\""));
        MAPA_CATEGORIAS.put(List.of("barbearia", "salao", "cabelo"),
                List.of("\"shop\"=\"hairdresser\"", "\"shop\"=\"beauty\""));
        MAPA_CATEGORIAS.put(List.of("padaria", "panificadora"),
                List.of("\"shop\"=\"bakery\""));
        MAPA_CATEGORIAS.put(List.of("academia", "fitness"),
                List.of("\"leisure\"=\"fitness_centre\""));
        MAPA_CATEGORIAS.put(List.of("hotel", "pousada"),
                List.of("\"tourism\"=\"hotel\"", "\"tourism\"=\"guest_house\""));
        MAPA_CATEGORIAS.put(List.of("advogado", "advocacia"),
                List.of("\"office\"=\"lawyer\""));
        MAPA_CATEGORIAS.put(List.of("contabilidade", "contador"),
                List.of("\"office\"=\"accountant\""));
        MAPA_CATEGORIAS.put(List.of("supermercado", "mercado"),
                List.of("\"shop\"=\"supermarket\""));
        MAPA_CATEGORIAS.put(List.of("petshop", "veterinario"),
                List.of("\"shop\"=\"pet\"", "\"amenity\"=\"veterinary\""));
    }

    public OpenStreetMapService(
            RestClient.Builder builder,
            ObjectMapper objectMapper
    ) {
        // Configura timeouts explícitos de conexão e leitura para o cliente HTTP
        // usado nas chamadas ao Overpass. Sem isso, uma requisição pode ficar
        // pendurada por minutos caso o servidor não responda nem com erro HTTP.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(OVERPASS_CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(OVERPASS_READ_TIMEOUT);

        this.restClient = builder
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "LeadlyApp/1.0 (contact: support@leadly.com)")
                .build();
        this.objectMapper = objectMapper;
    }

    public List<LeadSearchResultDTO> buscarLeads(
            String tipo,
            String pais,
            String estado,
            String localizacao
    ) {
        long startTime = System.currentTimeMillis();

        if (pais == null || pais.isBlank()) {
            throw new IllegalArgumentException("O campo 'país' é obrigatório.");
        }
        if (localizacao == null || localizacao.isBlank()) {
            throw new IllegalArgumentException("O campo 'localização/cidade' é obrigatório.");
        }
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("O campo 'tipo/categoria' é obrigatório.");
        }

        String paisClean = pais.trim();
        String estadoClean = estado == null ? "" : estado.trim();
        String localizacaoClean = localizacao.trim();

        log.info("Iniciando busca de leads. Categoria: '{}', Cidade: '{}', Estado: '{}', País: '{}'",
                tipo, localizacaoClean, estadoClean, paisClean);

        // 1. Converter País
        String codigoPais = converterPaisParaCodigo(paisClean);

        // 2. Buscar Coordenadas Geográficas
        Coordenadas coordenadas = buscarCoordenadasInteligente(
                localizacaoClean,
                estadoClean,
                paisClean,
                codigoPais
        );

        // 3. Mapear Categoria para Tags OSM Seguras
        List<String> tagsOSM = converterTipoParaTags(tipo);

        // 4. Montar a query Overpass
        String queryOverpass = montarQueryOverpass(tagsOSM, coordenadas);
        log.debug("Query Overpass gerada:\n{}", queryOverpass);

        // 5. Executar Requisição Overpass (Com Failover)
        String jsonResultado = executarOverpassComFailover(queryOverpass, tipo, localizacaoClean);

        if (jsonResultado == null || jsonResultado.isBlank()) {
            log.warn("Nenhum dado retornado do servidor Overpass.");
            return Collections.emptyList();
        }

        // 6. Processamento e Deduplicação dos Leads
        try {
            OverpassResponse response = objectMapper.readValue(jsonResultado, OverpassResponse.class);
            if (response.getElements() == null || response.getElements().isEmpty()) {
                log.info("Nenhum resultado encontrado no raio de busca.");
                return Collections.emptyList();
            }

            log.info("Overpass retornou {} elementos brutos para categoria='{}' localizacao='{}'.",
                    response.getElements().size(), tipo, localizacaoClean);

            List<LeadSearchResultDTO> leadsUnicos = processarEDeduplicarLeads(response.getElements(), tipo);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Busca concluída em {}ms. Leads válidos e únicos encontrados: {}", duration, leadsUnicos.size());

            return leadsUnicos;

        } catch (Exception e) {
            log.error("Falha ao desserializar resposta do Overpass JSON", e);
            throw new RuntimeException("Erro ao processar dados de localização do OpenStreetMap.", e);
        }
    }

    // =========================================================
    // MONTAGEM DA QUERY OVERPASS
    // =========================================================
    // Otimizações aplicadas em relação à versão anterior:
    //  - "nw" no lugar de "nwr": exclui relations (contornos/multipolígonos),
    //    que são muito mais caras de resolver e raramente carregam as tags
    //    de POIs comerciais como dentista/restaurante/farmácia etc.
    //  - Raio reduzido (ver RAIO_BUSCA_METROS) para diminuir a área varrida.
    //  - "out center N": limita a quantidade de elementos retornados, evitando
    //    respostas gigantes em cidades densas.
    //  - Locale.US explícito na formatação das coordenadas: sem isso, em uma
    //    JVM com locale pt_BR o separador decimal vira vírgula (ex: "-12,98"),
    //    o que quebra a sintaxe da query Overpass QL e gera erro no servidor
    //    (isso explica os 500 recebidos de dois dos três espelhos).
    private String montarQueryOverpass(List<String> tagsOSM, Coordenadas coordenadas) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("[out:json][timeout:")
                .append(OVERPASS_TIMEOUT_SEGUNDOS)
                .append("];\n(\n");

        for (String tag : tagsOSM) {
            queryBuilder.append(String.format(
                    Locale.US,
                    "  nw[%s](around:%d,%.6f,%.6f);\n",
                    tag,
                    RAIO_BUSCA_METROS,
                    coordenadas.latitude(),
                    coordenadas.longitude()
            ));
        }

        queryBuilder.append(");\nout center ")
                .append(MAX_RESULTADOS_OVERPASS)
                .append(";");

        return queryBuilder.toString();
    }

    // =========================================================
    // BUSCA ROBUSTA DE COORDENADAS (NOMINATIM)
    // =========================================================

    private Coordenadas buscarCoordenadasInteligente(
            String localizacao,
            String estado,
            String pais,
            String codigoPais
    ) {
        String estadoOficial = normalizarEstado(estado);

        // Estratégia de tentativas em cascata
        List<String> tentativas = new ArrayList<>();

        if (!estadoOficial.isEmpty()) {
            tentativas.add(localizacao + ", " + estadoOficial + ", " + pais);
            tentativas.add(removerAcentos(localizacao) + ", " + estadoOficial + ", " + pais);
        }
        tentativas.add(localizacao + ", " + pais);
        tentativas.add(removerAcentos(localizacao) + ", " + pais);

        for (int i = 0; i < tentativas.size(); i++) {
            String consulta = tentativas.get(i);
            log.debug("Tentativa {}/{} de geocodificação Nominatim: '{}'", i + 1, tentativas.size(), consulta);

            Coordenadas coords = consultarNominatimEValidar(
                    consulta,
                    codigoPais,
                    localizacao,
                    estadoOficial
            );

            if (coords != null) {
                log.info("Coordenadas localizadas com sucesso: Lat {}, Lon {}", coords.latitude(), coords.longitude());
                return coords;
            }
        }

        throw new IllegalArgumentException(
                String.format("Não foi possível localizar '%s' (%s, %s). Verifique os nomes informados.",
                        localizacao, estado, pais)
        );
    }

    private Coordenadas consultarNominatimEValidar(
            String consulta,
            String codigoPais,
            String cidadeBuscada,
            String estadoBuscado
    ) {
        URI uri = UriComponentsBuilder.fromUriString("https://nominatim.openstreetmap.org/search")
                .queryParam("q", consulta)
                .queryParam("format", "json")
                .queryParam("limit", 5)
                .queryParam("addressdetails", 1)
                .queryParam("countrycodes", codigoPais)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        try {
            String jsonResponse = restClient.get()
                    .uri(uri)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            if (jsonResponse == null || jsonResponse.isBlank()) return null;

            JsonNode root = objectMapper.readTree(jsonResponse);
            if (!root.isArray() || root.isEmpty()) return null;

            for (JsonNode item : root) {
                JsonNode address = item.get("address");
                if (address == null) continue;

                // Validar código ISO do País
                JsonNode countryCodeNode = address.get("country_code");
                if (countryCodeNode == null || !countryCodeNode.asText().equalsIgnoreCase(codigoPais)) {
                    continue;
                }

                // Validar Estado (caso tenha sido explicitamente informado)
                if (!estadoBuscado.isBlank()) {
                    String estadoEncontrado = extrairNomeEstado(address);
                    if (!validarEstadoCompativel(estadoBuscado, estadoEncontrado)) {
                        continue;
                    }
                }

                // Validar similaridade da Cidade
                String cidadeEncontrada = extrairNomeCidade(address, item);
                if (validarCidadeCompativel(cidadeBuscada, cidadeEncontrada)) {
                    double lat = item.get("lat").asDouble();
                    double lon = item.get("lon").asDouble();
                    return new Coordenadas(lat, lon);
                }
            }

        } catch (Exception e) {
            log.warn("Erro temporário ao consultar serviço Nominatim: {}", e.getMessage());
        }

        return null;
    }

    private boolean validarCidadeCompativel(String cidadeBuscada, String cidadeRetornada) {
        if (cidadeRetornada == null || cidadeRetornada.isBlank()) return false;

        String target = normalizarTexto(cidadeBuscada);
        String candidate = normalizarTexto(cidadeRetornada);

        if (target.equals(candidate)) return true;
        if (candidate.contains(target) || target.contains(candidate)) return true;

        double sim = calcularSimilaridade(target, candidate);
        return sim >= 0.75; // Threshold exigente para evitar falsos positivos (ex: São Paulo vs São José)
    }

    private boolean validarEstadoCompativel(String estadoBuscado, String estadoRetornado) {
        if (estadoRetornado == null || estadoRetornado.isBlank()) return true; // Se omitido no OSM, tolera
        String target = normalizarTexto(estadoBuscado);
        String candidate = normalizarTexto(estadoRetornado);
        return candidate.contains(target) || target.contains(candidate);
    }

    private String extrairNomeCidade(JsonNode address, JsonNode item) {
        String[] camposCidade = {"city", "town", "municipality", "village", "suburb", "county"};
        for (String campo : camposCidade) {
            if (address.has(campo) && !address.get(campo).asText().isBlank()) {
                return address.get(campo).asText();
            }
        }
        if (item.has("display_name")) {
            return item.get("display_name").asText().split(",")[0];
        }
        return "";
    }

    private String extrairNomeEstado(JsonNode address) {
        if (address.has("state")) return address.get("state").asText();
        if (address.has("region")) return address.get("region").asText();
        if (address.has("ISO3166-2-lvl4")) return address.get("ISO3166-2-lvl4").asText();
        return "";
    }

    // =========================================================
    // EXECUÇÃO DA OVERPASS API COM RETRY / FAILOVER
    // =========================================================
    // Tenta cada servidor da lista em sequência. Qualquer um dos casos abaixo
    // aciona o próximo servidor: erro HTTP (500/502/503/504/etc.), timeout de
    // leitura/conexão, ou qualquer outro erro de comunicação. Se todos falharem,
    // lança um erro genérico (sem stack trace) para o controller/handler global
    // devolver ao frontend.
    private String executarOverpassComFailover(String query, String tipo, String localizacao) {
        String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        for (int i = 0; i < OVERPASS_ENDPOINTS.size(); i++) {
            String endpoint = OVERPASS_ENDPOINTS.get(i);
            String proximoEndpoint = (i < OVERPASS_ENDPOINTS.size() - 1)
                    ? OVERPASS_ENDPOINTS.get(i + 1)
                    : "NENHUM (servidores esgotados)";

            long inicio = System.currentTimeMillis();

            try {
                String resposta = restClient.post()
                        .uri(endpoint)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body(body)
                        .retrieve()
                        .body(String.class);

                long duracao = System.currentTimeMillis() - inicio;
                log.info("Overpass OK | categoria='{}' localizacao='{}' servidor='{}' status=200 tempo={}ms",
                        tipo, localizacao, endpoint, duracao);

                return resposta;

            } catch (HttpStatusCodeException e) {
                long duracao = System.currentTimeMillis() - inicio;
                log.warn("Overpass FALHOU | categoria='{}' localizacao='{}' servidor='{}' status={} tempo={}ms " +
                                "motivo=HTTP_ERROR proximoServidor='{}'",
                        tipo, localizacao, endpoint, e.getStatusCode().value(), duracao, proximoEndpoint);

            } catch (ResourceAccessException e) {
                long duracao = System.currentTimeMillis() - inicio;
                log.warn("Overpass FALHOU | categoria='{}' localizacao='{}' servidor='{}' status=N/A tempo={}ms " +
                                "motivo=TIMEOUT_OU_CONEXAO erro='{}' proximoServidor='{}'",
                        tipo, localizacao, endpoint, duracao, e.getMessage(), proximoEndpoint);

            } catch (RestClientException e) {
                long duracao = System.currentTimeMillis() - inicio;
                log.warn("Overpass FALHOU | categoria='{}' localizacao='{}' servidor='{}' status=N/A tempo={}ms " +
                                "motivo=ERRO_GENERICO erro='{}' proximoServidor='{}'",
                        tipo, localizacao, endpoint, duracao, e.getMessage(), proximoEndpoint);
            }
        }

        log.error("Todos os servidores Overpass falharam | categoria='{}' localizacao='{}'", tipo, localizacao);
        throw new RuntimeException(
                "Serviço OpenStreetMap Overpass indisponível no momento. Tente novamente mais tarde."
        );
    }

    // =========================================================
    // FILTRAGEM E DEDUPLICAÇÃO DE LEADS
    // =========================================================

    private List<LeadSearchResultDTO> processarEDeduplicarLeads(List<OverpassElement> elementos, String categoriaBuscada) {
        List<LeadSearchResultDTO> leadsValidos = new ArrayList<>();
        Set<String> chavesUnicas = new HashSet<>();

        for (OverpassElement element : elementos) {
            Map<String, String> tags = element.getTags();
            if (tags == null) continue;

            // Decisão Arquitetural: Ignorar POIs sem nome comercial útil
            String nome = primeiroValor(tags, "name", "official_name", "brand");
            if (nome == null || nome.isBlank()) {
                continue;
            }

            Double lat = element.getLat();
            Double lon = element.getLon();

            if ((lat == null || lon == null) && element.getCenter() != null) {
                lat = element.getCenter().getLat();
                lon = element.getCenter().getLon();
            }

            if (lat == null || lon == null) continue;

            // Criar hash de unicidade: Nome Sanitizado + Coordenadas Arredondadas (Precisão ~100m)
            String geoHash = String.format(Locale.US, "%.3f_%.3f", lat, lon);
            String chaveUnica = normalizarTexto(nome) + "@" + geoHash;

            if (chavesUnicas.contains(chaveUnica)) {
                continue; // Ignorar elemento duplicado (Way/Node repetido)
            }
            chavesUnicas.add(chaveUnica);

            String telefone = primeiroValor(tags, "phone", "contact:phone", "mobile");
            String email = primeiroValor(tags, "email", "contact:email");
            String website = primeiroValor(tags, "website", "contact:website");
            String instagram = extrairInstagram(tags);
            String endereco = montarEndereco(tags);

            leadsValidos.add(new LeadSearchResultDTO(
                    nome.trim(),
                    telefone,
                    email,
                    website,
                    instagram,
                    endereco,
                    lat,
                    lon,
                    categoriaBuscada
            ));
        }

        return leadsValidos;
    }

    private String extrairInstagram(Map<String, String> tags) {
        String insta = primeiroValor(tags, "contact:instagram", "instagram", "facebook");
        if (insta == null) return null;

        insta = insta.trim();
        if (insta.startsWith("http://") || insta.startsWith("https://")) {
            return insta;
        }
        if (insta.startsWith("@")) {
            return "https://instagram.com/" + insta.substring(1);
        }
        return "https://instagram.com/" + insta;
    }

    private String primeiroValor(Map<String, String> tags, String... chaves) {
        for (String chave : chaves) {
            String v = tags.get(chave);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private String montarEndereco(Map<String, String> tags) {
        List<String> partes = new ArrayList<>();
        String rua = tags.get("addr:street");
        String numero = tags.get("addr:housenumber");
        String bairro = primeiroValor(tags, "addr:suburb", "addr:neighbourhood");
        String cidade = primeiroValor(tags, "addr:city", "addr:town");

        if (rua != null && !rua.isBlank()) {
            partes.add(numero != null && !numero.isBlank() ? rua.trim() + ", " + numero.trim() : rua.trim());
        }
        if (bairro != null && !bairro.isBlank()) partes.add(bairro.trim());
        if (cidade != null && !cidade.isBlank()) partes.add(cidade.trim());

        return partes.isEmpty() ? null : String.join(", ", partes);
    }

    // =========================================================
    // DICIONÁRIO E MAPEAMENTO DE CATEGORIAS
    // =========================================================
    // Estratégia em duas fases:
    //  1) Correspondência EXATA (substring), igual ao comportamento original,
    //     sempre tem prioridade.
    //  2) Se nenhuma exata for encontrada, tenta correspondência APROXIMADA
    //     (Levenshtein) contra as palavras-chave, com limiar alto (0.80) e
    //     tamanho mínimo de palavra, para tolerar erros de digitação do
    //     frontend (ex.: "dentisa" -> "dentista") sem gerar falsos positivos
    //     entre categorias diferentes.
    private List<String> converterTipoParaTags(String tipo) {
        String t = normalizarTexto(tipo);

        // Fase 1: correspondência exata
        for (Map.Entry<List<String>, List<String>> entry : MAPA_CATEGORIAS.entrySet()) {
            for (String palavraChave : entry.getKey()) {
                if (t.contains(palavraChave)) {
                    return entry.getValue();
                }
            }
        }

        // Fase 2: correspondência aproximada (tolerante a pequenos erros de digitação)
        String melhorPalavraChave = null;
        List<String> melhorTags = null;
        double melhorSimilaridade = 0.0;

        for (Map.Entry<List<String>, List<String>> entry : MAPA_CATEGORIAS.entrySet()) {
            for (String palavraChave : entry.getKey()) {
                if (palavraChave.length() < TAMANHO_MINIMO_PALAVRA_FUZZY) continue;

                double similaridade = calcularSimilaridade(t, palavraChave);
                if (similaridade > melhorSimilaridade) {
                    melhorSimilaridade = similaridade;
                    melhorPalavraChave = palavraChave;
                    melhorTags = entry.getValue();
                }
            }
        }

        if (melhorTags != null && melhorSimilaridade >= SIMILARIDADE_MINIMA_CATEGORIA) {
            log.warn("Categoria '{}' não encontrada exatamente. Usando correspondência aproximada '{}' (similaridade={}).",
                    tipo, melhorPalavraChave, String.format(Locale.US, "%.2f", melhorSimilaridade));
            return melhorTags;
        }

        // Se a categoria for desconhecida e não puder ser mapeada com segurança
        throw new IllegalArgumentException(
                String.format("A categoria '%s' não é suportada. Tente termos como: Dentista, Restaurante, Farmácia, Clínica, Academia, Hotel, Barbearia, etc.", tipo)
        );
    }

    // =========================================================
    // SUPORTE A PAÍSES E ESTADOS
    // =========================================================

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) return "";
        String stNorm = normalizarTexto(estado);

        if (stNorm.length() == 2) {
            String siglaUpper = stNorm.toUpperCase();
            if (ESTADOS_BR.containsKey(siglaUpper)) {
                return ESTADOS_BR.get(siglaUpper);
            }
        }

        for (Map.Entry<String, String> entry : ESTADOS_BR.entrySet()) {
            String nomeOficialNorm = normalizarTexto(entry.getValue());
            if (nomeOficialNorm.equals(stNorm)) {
                return entry.getValue();
            }
        }

        return estado.trim();
    }

    private String converterPaisParaCodigo(String pais) {
        String p = normalizarTexto(pais);

        return switch (p) {
            case "br", "brasil", "brazil" -> "br";
            case "us", "usa", "eua", "estados unidos", "united states" -> "us";
            case "pt", "portugal" -> "pt";
            case "es", "espanha", "espana", "spain" -> "es";
            case "ar", "argentina" -> "ar";
            case "cl", "chile" -> "cl";
            case "mx", "mexico" -> "mx";
            case "co", "colombia" -> "co";
            case "pe", "peru" -> "pe";
            case "uy", "uruguai", "uruguay" -> "uy";
            case "py", "paraguai", "paraguay" -> "py";
            case "ca", "canada" -> "ca";
            case "fr", "franca", "france" -> "fr";
            case "it", "italia", "italy" -> "it";
            case "de", "alemanha", "germany" -> "de";
            case "gb", "uk", "reino unido", "united kingdom" -> "gb";
            default -> {
                if (p.length() == 2) yield p.toLowerCase();
                throw new IllegalArgumentException("País não suportado: " + pais);
            }
        };
    }

    // =========================================================
    // UTILITÁRIOS DE TEXTO E LEVENSHTEIN
    // =========================================================

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String limpo = SANITIZE_PATTERN.matcher(texto).replaceAll("");
        return removerAcentos(limpo.trim().replaceAll("\\s+", " ").toLowerCase());
    }

    private String removerAcentos(String texto) {
        if (texto == null) return "";
        String nfd = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(nfd).replaceAll("");
    }

    private double calcularSimilaridade(String str1, String str2) {
        if (str1 == null || str2 == null) return 0.0;
        if (str1.equals(str2)) return 1.0;

        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) return 1.0;

        int distancia = calcularDistanciaLevenshtein(str1, str2);
        return 1.0 - ((double) distancia / maxLength);
    }

    private int calcularDistanciaLevenshtein(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) costs[j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(
                        1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1
                );
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    private record Coordenadas(double latitude, double longitude) {}
}