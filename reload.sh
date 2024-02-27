./gradlew clean xnatPluginJar

PLUGIN_DIR=/data/xnat/home/plugins
JAR_PREFIX=ldap-sync

rm $PLUGIN_DIR/$JAR_PREFIX*-xpl.jar
cp build/libs/ldap-sync-*-xpl.jar /data/xnat/home/plugins

brew services restart tomcat@9
