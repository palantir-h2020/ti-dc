package elastic;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import models.NetflowRecord;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.ingest.*;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;

/**
 * ElasticUtils class. Contains methods that may
 * be used for operations in elasticsearch for
 * inserting data.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class OpenSearchUtils {
    /**
     * Logger Instance.
     */
    private static Logger logger = Logger.getLogger(OpenSearchUtils.class);

    /**
     * High level Elastic REST client. It wraps low level client
     * for doing operations in Elastic.
     */
    private RestClient restClient;

    /**
     * OpenSearch client for doing operations in OpenSearch.
     */
    private OpenSearchClient client;

    /**
     * Processor rename pipeline, used for any renaming functionality.
     */
    private String PALANTIR_PIPELINE_RENAME = "palantir-netflow-rename-pipeline";
    /**
     * Processor convert pipeline, used for any data conversion.
     */
    private String PALANTIR_PIPELINE_CONVERT = "palantir-netflow-convert-pipeline";

    /**
     * Constructor. It creates a high level Elastic REST client,
     * for https methods. It skips self-signed certifications validation.
     * Simpe authentication using username and password is required.
     *
     * @param elasticHost     String Ip of Elastic host.
     * @param elasticPort     Integer Port number in Elastic port.
     * @param elasticUsername String Username for Authentication.
     * @param elasticPassword String Password for Authentication.
     */
    public OpenSearchUtils(String elasticHost, int elasticPort, String elasticUsername, String elasticPassword) {
        SSLContextBuilder sslBuilder = null;
        try {
            // Override validation error for Elastic's self-signed certification
            sslBuilder = SSLContexts.custom()
                    .loadTrustMaterial(null, (x509Certificates, s) -> true);
            final SSLContext sslContext = sslBuilder.build();
            ;

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(
                            elasticUsername,
                            elasticPassword
                    )
            );

            restClient = RestClient.builder(
                            new HttpHost(elasticHost, elasticPort, "https"))
                    .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                        @Override
                        public HttpAsyncClientBuilder customizeHttpClient(
                                HttpAsyncClientBuilder httpClientBuilder) {
                            return httpClientBuilder
                                    .setSSLContext(sslContext)
                                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                    .setDefaultCredentialsProvider(credentialsProvider);
                        }
                    }).build();

            OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            client = new OpenSearchClient(transport);

            // Initialize required pipelines
            initializeIngestionPipelines();
        } catch (KeyStoreException e) {
            logger.error("KeyStoreException. An error occurred trying to connect with Elastic client (https).");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        } catch (KeyManagementException e) {
            logger.error("KeyManagementException. An error occurred trying to connect with Elastic client (https).");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException. An error occurred trying to connect with Elastic client (https).");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }
    }

    private void initializeIngestionPipelines() {
        // Initialize Rename pipeline
        logger.info("Initializing " + PALANTIR_PIPELINE_RENAME + "!");
        if(!existsPipeline(PALANTIR_PIPELINE_RENAME)) {
            // Create list of processors, that will be used in pipeline
            List<Processor> renameProcessors = new ArrayList<Processor>();
            renameProcessors.add(new Processor.Builder()
                    .rename(new RenameProcessor.Builder()
                            .field("timestamp")
                            .targetField("@timestamp")
                            .build()
                    )
                    .build()
            );

            // Create pipeline
            createPipeline(PALANTIR_PIPELINE_RENAME, renameProcessors);
        }
    }

    /**
     * Initialize Elastic index. Check if the given index exists.
     * If it is not exists, create it. Number of shards and number
     * of partitions are default (equal to 1).
     *
     * @param indexName String Name of the index.
     * @return Boolean Returns true if the initialization completed
     * or the index already exists. Returns false, if the
     * index cannot be created (if it is not exists) and if
     * any error occurs.
     */
    public boolean initializeElasticIndex(String indexName) {
        boolean idxOk = initializeElasticIndex(indexName, 1, 1);

        return idxOk;
    }

    /**
     * Initialize Elastic index. Check if the given index exists.
     * If it is not exists, create it. Number of shards and number
     * of partitions are set from parameters.
     *
     * @param indexName     String Name of the index.
     * @param numShards     Integer Number of shards for this index.
     * @param numPartitions Integer Number of partitions for this index.
     * @return Boolean Returns true if the initialization completed
     * or the index already exists. Returns false, if the
     * index cannot be created (if it is not exists) and if
     * any error occurs.
     */
    public boolean initializeElasticIndex(String indexName, int numShards, int numPartitions) {
        boolean idxOk = true;

        if (!existsIndex(indexName)) {
            if (!createIndex(indexName, numShards, numPartitions)) {
                idxOk = false;
            }
        }

        return idxOk;
    }

    /**
     * Checks if a given index exists in Elastic.
     *
     * @param indexName String The name of the index to check.
     * @return Boolean Returns true, if index exists. Returns false
     * otherwise or if any error occurs.
     */
    private boolean existsIndex(String indexName) {
        boolean exists = false;

        try {
            ExistsRequest existsIdxReq = new ExistsRequest.Builder()
                    .index(indexName)
                    .local(false)
                    .includeDefaults(false)
                    .build();
            exists = client.indices().exists(existsIdxReq).value();
        } catch (IOException e) {
            logger.error("IOException. An error occurred trying to close Elastic client");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }

        logger.info("Searching for index " + indexName + ". Exists: " + exists);

        return exists;
    }

    /**
     * Creates a new index with specific number of shards and partitions.
     *
     * @param indexName     String Name of the index to be created.
     * @param numShards     Integer Number of shards to be used.
     * @param numPartitions Integer Number of partitions to be used.
     * @return Boolean Returns true if index successfully created. Returns false
     * if any error occurs.
     */
    private boolean createIndex(String indexName, int numShards, int numPartitions) {
        boolean idxCreated = false;

        IndexSettings indexSettings = new IndexSettings.Builder()
                .numberOfShards(Integer.toString(numShards))
                .numberOfReplicas(Integer.toString(numPartitions))
                .build();

        CreateIndexRequest createIdxReq = new CreateIndexRequest.Builder()
                .index(indexName)
                .settings(indexSettings)
                .build();

        try {
            client.indices().create(createIdxReq);
            idxCreated = true;
        } catch (IOException e) {
            logger.error("IOException. An error occurred trying to close Elastic client");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }

        logger.info("Creating index " + indexName + ". Successfully created: " + idxCreated);

        return idxCreated;
    }

    /**
     * Checks if a given pipeline exists in Elastic.
     *
     * @param pipelineName String The name of the pipeline to check.
     * @return Boolean Returns true, if index exists. Returns false
     * otherwise or if any error occurs.
     */
    private boolean existsPipeline(String pipelineName) {
        boolean exists = false;

        try {
            GetPipelineResponse getPipelineResponse = client.ingest().getPipeline(new GetPipelineRequest.Builder()
                    .id(pipelineName)
                    .build()
            );

            exists = getPipelineResponse.result().containsKey(pipelineName);
        }
        catch (IOException e) {
            logger.warn("IOException. An error occurred trying to search pipeline with timestamp rename. Creating anyway...");
            logger.warn(e.getCause());
            logger.warn(e.getMessage());
        }
        catch (OpenSearchException e) {
            logger.warn("OpenSearchException. An error occurred trying to search pipeline with timestamp rename. Creating anyway...");
            logger.warn(e.getCause());
            logger.warn(e.getMessage());
        }
        catch (Exception e) {
            logger.warn("Unknown Exception. An error occurred trying to search pipeline with timestamp rename. Creating anyway...");
            logger.warn(e.getCause());
            logger.warn(e.getMessage());
        }

        logger.info("Searching for pipeline " + pipelineName + ". Exists: " + exists);

        return exists;
    }

    /**
     * Creates a new pipeline.
     *
     * @param pipelineName     String Name of the pipeline to be created.
     * @return Boolean Returns true if index successfully created. Returns false
     * if any error occurs.
     */
    private boolean createPipeline(String pipelineName, List<Processor> pipelineProcessors) {
        boolean pipelineCreated = false;

        try {
            // Add pipeline with rename processor for rename timestamp field to @timestamp
            client.ingest().putPipeline(new PutPipelineRequest.Builder()
                    .id(pipelineName)
                    .processors(pipelineProcessors)
                    .build()
            );

            pipelineCreated = true;
        }
        catch (IOException e) {
            logger.error("IOException. An error occurred trying to create pipeline with timestamp rename");
            logger.error(e.getCause());
            logger.error(e.getMessage());
            logger.error("Timestamp cannot be converted. Netflow dashboards will not work as expected");
        }
        catch (OpenSearchException e) {
            logger.error("OpenSearchException. An error occurred trying to create pipeline with timestamp rename");
            logger.error(e.getCause());
            logger.error(e.getMessage());
            logger.error("Timestamp cannot be converted. Netflow dashboards will not work as expected");
        }
        catch (Exception e) {
            logger.error("Unknown Exception. An error occurred trying to create pipeline with timestamp rename");
            logger.error(e.getCause());
            logger.error(e.getMessage());
            logger.error("Timestamp cannot be converted. Netflow dashboards will not work as expected");
        }

        logger.info("Creating pipeline " + pipelineName + ". Successfully created: " + pipelineCreated);

        return pipelineCreated;
    }

    /**
     * Inserts a JSON document in an existing index with given Id.
     * Id must be unique in index.
     *
     * @param docId     String Id of the document, that will be inserted.
     * @param netflow   NetflowRecord NetflowRecord that will be inserted.
     * @param indexName String Name of the index, that the document will be inserted.
     * @return Returns true if the documented successfully inserted (or updated).
     * Returns false for any other result or if any error occurs.
     */
    public boolean insertNetflowRecord(String docId, NetflowRecord netflow, String indexName) {
        boolean docInserted = false;
        String docInsertedRes = "";

        //Index some data
        /*
        IndexRequest<NetflowRecord> indexRequest = new IndexRequest.Builder<NetflowRecord>()
                .index(indexName)
                .id(docId)
                .document(netflow)
                .pipeline(PALANTIR_PIPELINE_NAME)
                .build();
         */
        IndexRequest<NetflowRecord> indexRequest = new IndexRequest.Builder<NetflowRecord>()
                .index(indexName)
                .id(docId)
                .document(netflow)
                .build();

        IndexResponse indexRes = null;
        try {
            indexRes = client.index(indexRequest);

            if ((indexRes.result() == Result.Created) || (indexRes.result() == Result.Updated)) {
                docInserted = true;
                docInsertedRes = indexRes.result().jsonValue();
            }
        } catch (IOException e) {
            logger.error("IOException. An error occurred trying to insert document to Elastic");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        } catch (OpenSearchException e) {
            logger.error("OpensearchException. An error occurred trying to insert document to Elastic");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }
        catch (Exception e) {
            logger.error("Unknown Exception. An error occurred trying to insert document to Elastic");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }

        return docInserted;
    }

    /**
     * Closes high level Elastic REST client.
     */
    public void close() {
        try {
            restClient.close();
        } catch (IOException e) {
            logger.error("IOException. An error occurred trying to close Elastic client");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }
    }
}
