// Import express
let express = require('express');
let bodyParser = require('body-parser');
let compression = require('compression');
var cookieParser = require('cookie-parser');
let cors = require('cors');
let app = express();
var server = require('http').createServer(app);  
var io = require('socket.io')(server, { pingTimeout: 60000});


//API Gateway Port Config
let config = { express: { port: 9997, host: 'localhost'} }

if (!process.env.MOCHA_UNIT_TESTING) { 

    // Remove this in production? 
    app.set('json spaces', 4);

    app.use(bodyParser.urlencoded({
        extended: true
      }));

    app.use(function(req, res, next) {
        res.header("Access-Control-Allow-Origin", "*");
        res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, D-Hive-User");
        next();
    });      
    app.use(bodyParser.json());
    app.use(compression());
    app.use(express.static('../build'));
   
    app.use(cors());
    app.use(cookieParser());


    // Define main routes
    app.route('/api/getApps').get(getApps);
    app.route('/api/getGraphData').get(getGraphData);
    app.route('/api/getTestIds').post(getTestIds);

    app.all('*', (req, res, next) => {
        res.sendFile('index.html', {
            root: '../build'
        });
    });

    startExpress();

}


async function startExpress() {
    
    const server1 = server.listen(config.express.port);
    console.log('Listening at '+config.express.host+ ' on port '+config.express.port);
    server1.timeout = 60*60*1000;

    io.on('connection', function (client) {
        console.log('Client connected...', client.id);
        client.on('streamRmon', function (neIpObj) {
            console.log('stream for Ip:', neIpObj);
            //HealthMonitor.rmonStreamCurrentStateViaSocketIo(neIpObj, client);
        });
        client.on('disconnect', function (err) {
            console.log(client.id, "disconnected");
            client.disconnect(true);
        });
    });
}

async function getApps (req, res, next) {
    res.json ({apps: [
        'HR App',
        'Finance App'
    ]})
    next();
    return true;
}

async function getGraphData (req, res, next) {
    res.json({
        nodes: [
            {
                "data": {
                    "id": "movieinfo",
                    "text": "MovieInfo"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            },
            {
                "data": {
                    "id": "restwrapjdbc",
                    "text": "RestWrapJDBC"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            },
            {
                "data": {
                    "id": "details",
                    "text": "ProductDetails"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            },
            {
                "data": {
                    "id": "ratings",
                    "text": "ProductRatings"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            },
            {
                "data": {
                    "id": "reviews",
                    "text": "ProductReviews"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            }
        ],
        edges: [
            {
                id: 's1_s2',
                source: 'movieinfo',
                target: 'restwrapjdbc'
            },
            {
                id: 's1_s3',
                source: 'movieinfo',
                target: 'details'
            },
            {
                id: 's1_s4',
                source: 'movieinfo',
                target: 'ratings'
            },
            {
                id: 's1_s5',
                source: 'movieinfo',
                target: 'reviews'
            }
        ]
    })
    next();
    return true;
}

async function getTestIds (req, res, next) {
    let request = req.body;
    let testIds = [];
    for (let i = 0; i < 10; i++) {
        testIds.push(request.app.replace(' ', '-') + '-' + 1100 + i);
    }
    res.json ({ids: testIds})
    next();
    return true;
}

