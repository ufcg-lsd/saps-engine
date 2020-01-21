# Install and Configure Catalog

The Service Catalog component mantains the state of the tasks submitted to the SAPS. It is implemented as a plain PostgreSQL database. Here, we install and configure the required dependencies to run the PostgreSQL. The database schema will be created later during the execution of the Dispatcher component.

## Dependencies
In an apt-based Linux distro, type the below commands to install PostgreSQL packages.

  ```
  1. sudo apt-get update
  2. sudo apt-get install -y postgresql
  ```

## Configure

To configure SAPS catalog, first create an user and a database (```$catalog_user``` and ```$catalog_db_name```). You also need to define a password (```$catalog_passwd```) to access the database. See below commands:

  ```
  1. sudo su
  2. su postgres
  3. psql -c "CREATE USER $catalog_user WITH PASSWORD '$catalog_passwd';"
  4. psql -c "CREATE DATABASE $catalog_db_name OWNER $catalog_user;"
  5. psql -c "GRANT ALL PRIVILEGES ON DATABASE $catalog_db_name TO $catalog_user;"
  ```

Once the database was created, you need to grant access to external clients:

  ```
  1. sed -i 's/peer/md5/g' /etc/postgresql/<installed_version>/main/pg_hba.conf
  2. bash -c 'echo "host all all 0.0.0.0/0 md5" >> /etc/postgresql/<installed_version>/main/pg_hba.conf'
  3. sudo sed -i "$ a\listen_addresses = '*'" /etc/postgresql/<installed_version>/main/postgresql.conf
  4. sudo service postgresql restart
  ```

## Test
To test the deploy and configuration of the SAPS catalog (assuming it is accessible via the ```$catalog_ip_address``` IP address), one can try opening a conection to it from another machine (it is necessary to have PostgreSQL installed), using the below command:

```
psql -h $catalog_ip_address -p 5432 $catalog_db_name $catalog_user
```

Note that, we are using the default PostgreSQL database port, 5432, and the Ingress traffic had to allowed previously. Also, note that the PostgreSQL dependencies are required to run the above command.
