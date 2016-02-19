cd ../../ServiceDescription/environment-management-rpm/target/rpm/environment-management-rpm/RPMS/noarch
rm -rf /opt/environment-management
RPM_FILE=`find . -maxdepth 1 -type f`
rpm -e environment-management-rpm
rpm -i $RPM_FILE

chmod -R +rx /opt/environment-management
chmod +rx /opt/environment-management/config
chmod +r /opt/environment-management/config/config.properties
chmod +rx /opt/environment-management/bin
cp /home/scowx/apps/config.properties /opt/environment-management/config/config.properties

