#!/bin/sh
if [ "x$JBOSS_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    echo "Set ENV JBOSS_HOME!"
    return 1
fi
#mvn clean install
# -Dversion.jboss.legacy=5.1.0.GA ... eap build does not produce artifacts...
# -Dcheckstyle.skip=true - ... signed jars screw checkstyle
mvn clean install -Dversion.jboss.legacy=5.1.0.GA -Dcheckstyle.skip=true
#mvn clean install -Dversion.jboss.legacy=5.3.0-SNAPSHOT

cp -Rf lib/target/module/org $JBOSS_HOME/modules/system/layers/base/
cp -Rf jnp/target/module/org $JBOSS_HOME/modules/system/layers/base/
cp -Rf ejb3/target/module/org $JBOSS_HOME/modules/system/layers/base/
cp -Rf ejb3-bridge/target/module/org $JBOSS_HOME/modules/system/layers/base/
cp -Rf tx/target/module/org $JBOSS_HOME/modules/system/layers/base/


echo "Edit configuration file - for instance $JBOSS_HOME/standalone/configuration/standalone.xml"

echo "To enable JNP:"
echo "1 Add extension definition in <extensions>."
echo "<extension module=\"org.jboss.legacy.jnp\"/>"
echo
echo "2. Add sdubsystem definition"
echo "<subsystem xmlns=\"urn:jboss:domain:legacy-jnp:1.0\">"
echo "    <jnp-server/>"
echo "    <jnp-connector socket-binding=\"jnp\" rmi-socket-binding=\"rmi-jnp\" />"
echo "    <remoting socket-binding=\"legacy-remoting\"/>"
echo "</subsystem>"
echo 
echo "3. Define a socket-binding for the JNP Server using the 'jnp' name"
echo "<socket-binding name=\"jnp\" port=\"5599\"/>"
echo
echo "4. You can also define a RMI binding socket using the 'rmi-jnp' name (Optionnal if you don't have declared it in the jnp-connector)"
echo "<socket-binding name=\"rmi-jnp\" port=\"1099\"/>"
echo
echo "5. Define a socket-binding for the remoting using the 'legacy-remoting' name"
echo "<socket-binding name=\"legacy-remoting\" port=\"4873\"/>"
echo
echo "5. If you want to configure a HA JNDI JNP Server, you can add :"
echo "<distributed-cache cache-ref=\"default\" cache-container=\"singleton\"/>"
echo
echo "To enable EJB3:"
echo "1 Add extension definition in <extensions>."
echo
echo "<extension module=\"org.jboss.legacy.ejb3\"/>"
echo
echo "2. Add subsystem definition(no args == default IP/port)"
echo
echo "<subsystem xmlns=\"urn:jboss:domain:legacy-ejb3:1.0\">"
echo "    <ejb3-registrar/>"
echo "</subsystem>"
echo
echo "To enable EJB3-Bridge:"
echo "1 Add extension definition in <extensions>."
echo
echo "<extension module=\"org.jboss.legacy.ejb3.bridge\"/>"
echo
echo "2. Add subsystem definition(no args == default IP/port)"
echo
echo "<subsystem xmlns=\"urn:jboss:domain:legacy-ejb3-bridge:1.0\"/>"
echo
echo
echo "To enable User-transaction:"
echo "1 Add extension definition in <extensions>."
echo
echo "<extension module=\"org.jboss.legacy.tx\"/>"
echo
echo "2. Add subsystem definition"
echo
echo "<subsystem xmlns=\"urn:jboss:domain:legacy-tx:1.0\"/>"
echo
