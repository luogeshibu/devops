FROM docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4
RUN elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v6.2.4/elasticsearch-analysis-ik-6.2.4.zip
ADD IKAnalyzer.cfg.xml /usr/share/elasticsearch/config/analysis-ik
ADD java.policy /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.161-0.b14.el7_4.x86_64/jre/lib/security
RUN chown elasticsearch:root /usr/share/elasticsearch/config/analysis-ik/IKAnalyzer.cfg.xml
RUN echo "thread_pool.index.size: 5" >> /usr/share/elasticsearch/config/elasticsearch.yml
RUN echo "thread_pool.index.queue_size: 1000" >> /usr/share/elasticsearch/config/elasticsearch.yml
