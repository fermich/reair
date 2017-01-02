package com.airbnb.reair.incremental.configuration;

import com.airbnb.reair.common.HiveMetastoreClient;
import com.airbnb.reair.common.HiveMetastoreException;
import com.airbnb.reair.common.SaslHiveMetastoreClient;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;

import java.net.URI;

public class SaslCluster implements Cluster {

  private String name;
  private String metastoreHost;
  private int metastorePort;
  private String jobtrackerHost;
  private String jobtrackerPort;
  private Path hdfsRoot;
  private Path tmpDir;
  private final String metastorePrincipal;
  private final String tokenSignature;
  private ThreadLocal<SaslHiveMetastoreClient> metastoreClient;

  /**
   * Constructor with specific values.
   *
   * @param name string to use for identifying this cluster
   * @param metastoreHost hostname of the metastore Thrift server
   * @param metastorePort port of the metastore Thrift server
   * @param jobtrackerHost hostname of the job tracker
   * @param jobtrackerPort port of the job tracker
   * @param hdfsRoot the path for the root HDFS directory
   * @param tmpDir the path for the temporary HDFS directory (should be under root)
   * @param metastorePrincipal the service principal for the metastore thrift server
   * @param tokenSignature string used to store delegation token
   */
  public SaslCluster(
      String name,
      String metastoreHost,
      int metastorePort,
      String jobtrackerHost,
      String jobtrackerPort,
      Path hdfsRoot,
      Path tmpDir,
      String metastorePrincipal,
      String tokenSignature) {
    this.name = name;
    this.metastoreHost = metastoreHost;
    this.metastorePort = metastorePort;
    this.jobtrackerHost = jobtrackerHost;
    this.jobtrackerPort = jobtrackerPort;
    this.hdfsRoot = hdfsRoot;
    this.tmpDir = tmpDir;
    this.metastorePrincipal = metastorePrincipal;
    this.tokenSignature = tokenSignature;
    this.metastoreClient = new ThreadLocal<SaslHiveMetastoreClient>();
  }

  public String getMetastoreHost() {
    return metastoreHost;
  }

  public int getMetastorePort() {
    return metastorePort;
  }

  /**
   * Get a cached ThreadLocal metastore client.
   */
  @Override
  public HiveMetastoreClient getMetastoreClient() throws HiveMetastoreException {
    SaslHiveMetastoreClient result = this.metastoreClient.get();
    if (result == null) {
      try {
        URI uri = new URI("thrift://" + getMetastoreHost() + ":" + getMetastorePort());
        IMetaStoreClient metastoreClient = SaslClusterUtils.getMetastoreClient(uri, tokenSignature, metastorePrincipal, new Configuration());
        result = new SaslHiveMetastoreClient(metastoreClient);
        this.metastoreClient.set(result);
      } catch (Exception ex) {
        throw new HiveMetastoreException(ex);
      }
    }
    return result;
  }

  public Path getFsRoot() {
    return hdfsRoot;
  }

  public Path getTmpDir() {
    return tmpDir;
  }

  public String getName() {
    return name;
  }
}