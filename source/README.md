# QT21 #

## Project ##
This project is the web app of QT21, written in scala with Play framework

### Debug ###
run
```
./activator run
```
to start the app.

### Deploying on server ###

In order to deploy the app on the server, one needs to first create an archive for production use.

```
cd project_folder
./activator dist
```

Then copy the created archive to your destination folder

```
scp target/universal/qt21-1.0-SNAPSHOT.zip username@your.host.name:/your/destination/folder/
```

and then log into server, unzip the zip file and run ./bin/qt21 to start the server.
```
ssh username@your.host.name
cd /your/destination/folder
unzip qt21-1.0-SNAPSHOT.zip
cd qt21-1.0-SNAPSHOT
nohup ./bin/qt21 &
```

##### Note: #####
* you might want to remove the old qt21-1.0-SNAPSHOT.zip and qt21-1.0-SNAPSHOT folder before new deployment.
* run with nohup to ensure the app keeps running after you log out.
* the server might stop sometimes for unknown reason, to restart the app just run ```nohup ./bin/qt21 &``` again. If it tells you the program is already running, just delete the file RUNNING_PID as it tells you.

