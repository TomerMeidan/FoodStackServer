# FoodStack Server Side
## About
The Server Side is responsible for handling all server-side operations required by the system. 
This includes handling requests from clients, managing the database, and performing various business logic operations.

## Set Up
To set up the Server Side, follow these steps:
* Install a relational database management system (RDBMS) such as MySQL or PostgreSQL on the server machine.
* Create a new database schema using the provided SQL script, which will create all the necessary tables and relations.
* Configure the database connection settings in the Server Side code to connect to the database.
* Run the Server Side code on the server machine, listening for incoming client requests.
The Server Side code is written in Java and uses the Spring Framework for managing requests and database connections. 
It also uses JDBC for performing database operations.

To customize the Server Side, you can modify the Spring configuration files to change the server settings and modify 
the Java code to add new features or change existing ones.

Please note that the Server Side should be run on a secure and protected server to prevent unauthorized access to sensitive data.
