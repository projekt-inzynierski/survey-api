# How to start the project as a developer?

If you inspect `main/resources/application.properties` you can see that there are some variables used in the application. They are taken from environmental variables, but some of them have default values. Those that don't have default values have to be set by a developer. These are:
- `SPRING_FLYWAY_USER` - database username for flyway (most likely sa in develobpemnt environment)
- `SPRING_FLYWAY_PASSWORD` - database password for flyway
- `SPRING_DATASOURCE_PASSWORD` - database password
- `SPRING_DATASOURCE_USER` - database username for flyway (most likely sa in develobpemnt environment)
- `SPRING_DATASOURCE_URL` - database url (a connection string to your database)

The best idea is to configure your IDE, so that it sets those variables always, when you run the application. 

## IntelliJ Idea instruction

To configure your IntelliJ Idea to the following:
- On the top mane open `Run` context menu
- Go to `Debug` -> `Edit configurations` -> `Edit environmental variables`
- Add proper variables with values and save changes

## Documentation
- Run this API
- Go to http://[host]:[port]/swagger-ui.html