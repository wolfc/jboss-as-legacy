#!/bin/sh
if [ "x$JBOSS_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    echo "Set ENV JBOSS_HOME!"
    return 1
fi
mvn clean install
cp -Rf lib/target/module/org $JBOSS_HOME/modules/system/layers/base/
cp -Rf jnp/target/module/org $JBOSS_HOME/modules/system/layers/base/
#cp -Rf ejb3/target/module/org $JBOSS_HOME/modules/system/layers/base/

echo "Edit configuration file - for instance $JBOSS_HOME/standalone/configuration/standalone.xml"

echo "To enable JNP:"
echo "1 Add extension definition in <extensions>."
echo "<extension module="org.jboss.legacy.jnp"/>"
echo
echo "2. Add sdubsystem definition(no args == default IP/port)"
echo "<subsystem xmlns=\"urn:jboss:domain:legacy-jnp:1.0\">"
echo "    <jnp-server/>"
echo "    <jnp-connector/>"
echo "</subsystem>"
echo 
echo
echo "To enable EJB3:"
echo "1 Add extension definition in <extensions>."
echo
echo "<extension module="org.jboss.legacy.ejb3"/>"
echo
echo
echo "2. Add subsystem definition(no args == default IP/port)"
echo
echo "<subsystem xmlns=\"urn:jboss:domain:legacy-ejb3:1.0\">"
echo "    <remoting/>"
echo "    <ejb3-registrar/>"
echo "</subsystem>"