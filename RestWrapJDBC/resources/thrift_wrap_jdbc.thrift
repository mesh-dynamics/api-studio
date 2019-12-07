include "jaeger.thrift"

namespace java com.cubeio.thriftwrapjdbc

exception GenericThriftWrapException {
  1: string message;
}

exception PoolCreationException {
  1: string message;
}

service ThriftWrapJDBC{
  string health(1: jaeger.Span span);
  string initialize(1: string username, 2: string password, 3: string uri, 4: jaeger.Span span) throws (1: PoolCreationException jdbcPoolException);
  string query(1: string query, 2: string params, 3: jaeger.Span span) throws (1: GenericThriftWrapException genericException);
  string update(1: string queryAndParam, 2: jaeger.Span span) throws (1: GenericThriftWrapException genericException);
}