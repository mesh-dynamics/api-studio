## Steps to Deploy Moviebook/cube App

The Deploy script take 3 parameters:  
1) appname   
2) operation   
3) Configuration

syntax:  
```
./deploy.sh appname operation Configuration
```


### 1) Appname  
Appname could be either `moviebook` or `cube`.

### 2) Operation
Operation could be one of the following:  

a) `init`  
Generate yamls and deploy app.  

b) `setup_record`  
Generate and deploy envoy filters for record.

c) `record`  
Make API calls to start recording.

d) `stop_record`  
Delete envoy filters and make API call to stop recording.

e) `setup_replay`  
Generate and deploy replay files.

f) `replay`  
Make API calls to init and start replay.

g) `stop_replay`  
Delete replay envoy filters.

h) `analyze`  
Make API call to start analyze

i) `clean`  
This will delete the namespace and all the resources in that namespace.

### 3) Configuration
Configuration could be `staging` or `dogfooding`.  
Configuration decides where and how to deploy the App.


## Example to illustrate how to setup apps and run dogfooding

1. Deploy moviebook App in staging environment  
```
./deploy.sh moviebook init staging
```

2. Deploy Cube App in staging environment  
```
./deploy.sh cube init staging
```

3. Deploy Cube App in  dogfooding environment  
```
./deploy.sh cube init dogfooding
```

4. Start recording Cube in staging environment  
```
./deploy.sh cube record staging
```

5. Start recording moviebook in staging environment  
```
./deploy.sh moviebook record staging
```

6. Make request to Moviebook app  
```
curl -X GET 'http://staging2.cubecorp.io/minfo/listmovies?filmName=BEVERLY%20OUTLAW' -H 'Content-Type: application/x-www-form-urlencoded' -H 'cache-control: no-cache'
```

7. Stop recording moviebook App  
```
./deploy.sh moviebook stop_record staging
```

8. Setup replay on moviebook App  
```
./deploy.sh moviebook setup_replay staging
```

9. Replay Moviebook  
```
./deploy.sh moviebook replay staging
```

10. Stop replay on moviebook  
```
./deploy.sh moviebook stop_replay staging
```

11. Run analyze on moviebook  
```
./deploy.sh moviebook analyze staging
```

12. Stop recording cube on staging  
```
./deploy.sh cube stop_record staging
```

13. Setup replay on cube of staging environment  
```
./deploy.sh cube setup_replay staging
```

14. Run replay on cube of staging environment  
```
./deploy.sh cube replay staging
```

15. Run analyze on cube of dogfooding environment  
```
./deploy.sh cube analyze dogfooding
```

16. Delete replay filter form cube on staging environment  
```
./deploy.sh cube stop_replay staging
```
