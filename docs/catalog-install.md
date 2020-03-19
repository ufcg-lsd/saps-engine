# Install and Configure Catalog

The Service Catalog component mantains the state of the tasks submitted to the SAPS. It is implemented as a plain PostgreSQL database. Here, we install and configure the required dependencies to run the PostgreSQL. The database schema will be created later during the execution of the Dispatcher component.

## Dependencies
In an apt-based Linux distro, type the below commands to install PostgreSQL packages.

  ```bash
  sudo apt-get update
  sudo apt-get install -y postgresql
  ```

## Configure

To configure SAPS catalog, first create an user, defined by a name (```catalog_user```) and password (```catalog_passwd```). In addition, create a database (```catalog_db_name```). Fill the variables and run below commands:

  ```
  sudo su
  su postgres
  catalog_user=<user_name>
  catalog_db_name=<db_name>
  catalog_passwd=<password>
  psql -c "CREATE USER $catalog_user WITH PASSWORD '$catalog_passwd';"
  psql -c "CREATE DATABASE $catalog_db_name OWNER $catalog_user;"
  psql -c "GRANT ALL PRIVILEGES ON DATABASE $catalog_db_name TO $catalog_user;"
  exit
  ```

Once the database was created, you need to grant access to external clients. For that, we need to know which version of Postgres is installed.
Run command below to check the posgresql version: 
  ```
  ls /etc/postgresql
  ```
Run commands below to grant access to external clients:

  ```
  installed_version=<postgres_version>
  sed -i 's/peer/md5/g' /etc/postgresql/$installed_version/main/pg_hba.conf
  bash -c 'echo "host all all 0.0.0.0/0 md5" >> /etc/postgresql/$installed_version/main/pg_hba.conf'
  sudo sed -i "$ a\listen_addresses = '*'" /etc/postgresql/$installed_version/main/postgresql.conf
  sudo service postgresql restart
  ```

## Test
To verify whether the deploy was successful, one can try opening a connection (assuming it is accessible via the ```$catalog_ip_address``` IP address) to it from another machine (it is necessary to have PostgreSQL installed), using the below command:

```
psql -h $catalog_ip_address -p 5432 $catalog_db_name $catalog_user
```

Note that, we are using the default PostgreSQL database port, 5432, and the **Ingress traffic had to allowed** previously. Also, note that the PostgreSQL dependencies are required to run the above command.
