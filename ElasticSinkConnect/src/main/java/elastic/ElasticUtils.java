package elastic;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

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

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;

import javax.net.ssl.SSLContext;

/**
 * ElasticUtils class. Contains methods that may
 * be used for operations in elasticsearch for
 * inserting data.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class ElasticUtils {
    /**
     * Logger Instance.
     */
    private static Logger logger = Logger.getLogger(ElasticUtils.class);

    /**
     * High level Elastic REST client. It wraps low level client
     * for doing operations in Elastic.
     */
    private RestHighLevelClient restHighLevelClient;

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
    public ElasticUtils(String elasticHost, int elasticPort, String elasticUsername, String elasticPassword) {
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

            // Connect to Elastic given provided credentials
            RestClientBuilder builder = RestClient.builder(
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
                    });

            restHighLevelClient = new RestHighLevelClient(builder);
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
        return initializeElasticIndex(indexName, 1, 1);
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
            GetIndexRequest existsIdxReq = new GetIndexRequest(indexName);
            existsIdxReq.local(false);
            existsIdxReq.humanReadable(true);
            existsIdxReq.includeDefaults(false);

            exists = restHighLevelClient.indices().exists(existsIdxReq, RequestOptions.DEFAULT);
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

        CreateIndexRequest createIdxReq = new CreateIndexRequest(indexName);
        createIdxReq.settings(Settings.builder()
                .put("index.number_of_shards", numShards)
                .put("index.number_of_replicas", numPartitions)
        );

        try {
            CreateIndexResponse createIdxRes = restHighLevelClient.indices().create(
                    createIdxReq, RequestOptions.DEFAULT
            );
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
     * Inserts a JSON document in an existing index.
     *
     * @param jsonDoc   String String representation of a JSON document.
     * @param indexName String Name of the index, that the document will be inserted.
     * @return Boolean Returns true if the documented successfully inserted (or updated).
     * Returns false for any other result or if any error occurs.
     */
    public boolean insertJsonDoc(String jsonDoc, String indexName) {
        boolean docInserted = false;

        try {
            IndexRequest insertIdxReq = new IndexRequest(indexName);
            insertIdxReq.source(jsonDoc, XContentType.JSON);

            IndexResponse indexRes = restHighLevelClient.index(insertIdxReq, RequestOptions.DEFAULT);

            if ((indexRes.getResult() == DocWriteResponse.Result.CREATED)
                    || (indexRes.getResult() == DocWriteResponse.Result.UPDATED)
            ) {
                docInserted = true;
            }
        } catch (IOException e) {
            logger.error("IOException. An error occurred trying to insert document to Elastic");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }

        return docInserted;
    }

    /**
     * Inserts a JSON document in an existing index with given Id.
     * Id must be unique in index.
     *
     * @param jsonDoc   String String representation of a JSON document.
     * @param docId     String Id of the document, that will be inserted.
     * @param indexName String Name of the index, that the document will be inserted.
     * @return Returns true if the documented successfully inserted (or updated).
     * Returns false for any other result or if any error occurs.
     */
    public boolean insertJsonDoc(String jsonDoc, String docId, String indexName) {
        boolean docInserted = false;

        try {
            IndexRequest insertIdxReq = new IndexRequest(indexName);
            insertIdxReq.id(docId);
            insertIdxReq.source(jsonDoc, XContentType.JSON);

            IndexResponse indexRes = restHighLevelClient.index(insertIdxReq, RequestOptions.DEFAULT);

            if ((indexRes.getResult() == DocWriteResponse.Result.CREATED)
                    || (indexRes.getResult() == DocWriteResponse.Result.UPDATED)
            ) {
                docInserted = true;
            }
        } catch (IOException e) {
            logger.error("IOException. An error occurred trying to insert document to Elastic");
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
            restHighLevelClient.close();
        } catch (IOException e) {
            logger.error("IOException. An error occurred trying to close Elastic client");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }
    }
}
