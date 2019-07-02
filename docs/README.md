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

a) init  
Generate yamls and deploy app.  

b) record  
Generate envoy filters for record and make API calls to start recording.

c) stop_record  
Delete envoy filters and make API call to stop recording.

d) setup_replay  
Generate and deploy replay files.

e) replay  
Make API calls to init and start replay.

f) stop_replay  
Delete replay envoy filters.

g) analyze  
Make API call to start analyze

h) clean  
This will delete the namespace and all the resources in that namespace.

### 3) Configuration
Configuration could be `staging` or `dogfooding`.  
Configuration decides where and how to deploy the App.


## Example to illustrate how to setup apps and run dogfooding

1. Deploy moviebook App in staging environment  
```
./deploy moviebook init staging
```

2. Deploy Cube App in staging environment  
```
./deploy cube init staging
```

3. Deploy Cube App in  dogfooding environment  
```
./deploy cube init dogfooding
```

4. Start recording Cube in staging environment  
```
./deploy cube record staging
```

5. Start recording moviebook in staging environment  
```
./deploy moviebook record staging
```

6. Make request to Moviebook app  
```
curl -X GET 'http://staging2.cubecorp.io/minfo/listmovies?filmName=BEVERLY%20OUTLAW' -H 'Content-Type: application/x-www-form-urlencoded' -H 'cache-control: no-cache'
```

7. Stop recording moviebook App  
```
./deploy moviebook stop_record staging
```

8. Setup replay on moviebook App  
```
./deploy moviebook setup_replay staging
```

9. Replay Moviebook  
```
./deploy moviebook replay staging
```

10. Stop replay on moviebook  
```
./deploy moviebook stop_replay staging
```

11. Run analyze on moviebook  
```
./deploy moviebook analyze staging
```

12. Stop recording cube on staging  
```
./deploy cube stop_record staging
```

13. Setup replay on cube of staging environment  
```
./deploy cube setup_replay staging
```

14. Run replay on cube of staging environment  
```
./deploy cube replay staging
```

15. Run analyze on cube of dogfooding environment  
```
./deploy cube analyze dogfooding
```

16. Delete replay filter form cube on staging environment  
```
./deploy cube stop_replay staging
```
