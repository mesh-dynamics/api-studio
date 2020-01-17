#include <nginx.h>
#include <ngx_config.h>
#include <ngx_core.h>
#include <ngx_http.h>


typedef struct {
    ngx_msec_t    latency;
    ngx_flag_t    status_1xx;
    ngx_flag_t    status_2xx;
    ngx_flag_t    status_3xx;
    ngx_flag_t    status_4xx;
    ngx_flag_t    status_5xx;
    size_t        buffer_size;
    // ADDED FOR CUBE : keeping header buffer size different from body buffer size
    size_t        header_buffer_size;
    ngx_flag_t    capture_body;
    ngx_str_t     capture_body_var;
    ngx_array_t  *conditions;
    ngx_array_t  *cv;
} ngx_http_response_body_loc_conf_t;


typedef struct {
    ngx_http_response_body_loc_conf_t   *blcf;
    ngx_buf_t                            buffer;
    // ADDED FOR CUBE : two more buffers for keeping req and resp headers
    ngx_buf_t                            req_header_buffer;
    ngx_buf_t                            resp_header_buffer;
    ngx_buf_t                            header_value_buffer;
} ngx_http_response_body_ctx_t;


static ngx_int_t
ngx_http_response_body_add_variables(ngx_conf_t *cf);

// ADDED FOR CUBE : Common fuction to transfer contents of a buffer to an embedded variable
static ngx_int_t transfer_buffer_to_variable(ngx_http_variable_value_t *v,
  ngx_buf_t *b);

static ngx_int_t
ngx_http_response_body_variable(ngx_http_request_t *r,
    ngx_http_variable_value_t *v, uintptr_t data);

// ADDED FOR CUBE : functions to set embedded variables for headers from context buffers
static ngx_int_t
ngx_http_request_header_variable(ngx_http_request_t *r,
    ngx_http_variable_value_t *v, uintptr_t data);

static ngx_int_t
ngx_http_response_header_variable(ngx_http_request_t *r,
        ngx_http_variable_value_t *v, uintptr_t data);

static void *ngx_http_response_body_create_loc_conf(ngx_conf_t *cf);
static char *ngx_http_response_body_merge_loc_conf(ngx_conf_t *cf, void *parent,
    void *child);

static char *
ngx_http_response_body_request_var(ngx_conf_t *cf, ngx_command_t *cmd,
    void *conf);

static ngx_int_t
ngx_http_response_body_set_ctx(ngx_http_request_t *r);

static ngx_int_t
ngx_http_response_body_log(ngx_http_request_t *r);

static ngx_int_t ngx_http_response_body_filter_header(ngx_http_request_t *r);
static ngx_int_t ngx_http_response_body_filter_body(ngx_http_request_t *r,
    ngx_chain_t *in);

static ngx_int_t ngx_http_response_body_init(ngx_conf_t *cf);

static char *
ngx_conf_set_flag(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);

static char *
ngx_conf_set_msec(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);

static char *
ngx_conf_set_size(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);

static char *
ngx_conf_set_keyval(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);

static ngx_int_t allocate_buffer_if_already_not(ngx_buf_t *b, size_t buffer_size,ngx_http_request_t *r);

static ngx_int_t copy_headers_to_buffer(ngx_buf_t *b, ngx_list_part_t* part
  , ngx_http_request_t *r, ngx_buf_t *tmp_buf);

static size_t escape_special_char(ngx_buf_t* target_buf, u_char* source,
    size_t source_len);

static ngx_http_output_header_filter_pt  ngx_http_next_header_filter;
static ngx_http_output_body_filter_pt    ngx_http_next_body_filter;


static ngx_int_t
ngx_http_next_header_filter_stub(ngx_http_request_t *r)
{
    return NGX_OK;
}


static ngx_int_t
ngx_http_next_body_filter_stub(ngx_http_request_t *r, ngx_chain_t *in)
{
    return NGX_OK;
}


static ngx_command_t  ngx_http_response_body_commands[] = {

    { ngx_string("capture_response_body"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_flag,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, capture_body),
      NULL },

    { ngx_string("capture_response_body_var"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_http_response_body_request_var,
      NGX_HTTP_LOC_CONF_OFFSET,
      0,
      NULL },

    { ngx_string("capture_response_body_if"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE2,
      ngx_conf_set_keyval,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, conditions),
      NULL },

    { ngx_string("capture_response_body_if_1xx"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_flag,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, status_1xx),
      NULL },

    { ngx_string("capture_response_body_if_2xx"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_flag,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, status_2xx),
      NULL },

    { ngx_string("capture_response_body_if_3xx"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_flag,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, status_3xx),
      NULL },

    { ngx_string("capture_response_body_if_4xx"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_flag,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, status_4xx),
      NULL },

    { ngx_string("capture_response_body_if_5xx"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_flag,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, status_5xx),
      NULL },

    { ngx_string("capture_response_body_if_latency_more"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_msec,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, latency),
      NULL },

    { ngx_string("capture_response_body_buffer_size"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_size,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, buffer_size),
      NULL },

    // ADDED FOR CUBE : setting header buffer size in location conf from nginx config
    { ngx_string("capture_header_buffer_size"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_size,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_response_body_loc_conf_t, header_buffer_size),
      NULL },

      ngx_null_command

};


static ngx_http_module_t  ngx_http_response_body_module_ctx = {
    ngx_http_response_body_add_variables,    /* preconfiguration */
    ngx_http_response_body_init,             /* postconfiguration */

    NULL,                                    /* create main configuration */
    NULL,                                    /* init main configuration */

    NULL,                                    /* create server configuration */
    NULL,                                    /* merge server configuration */

    ngx_http_response_body_create_loc_conf,  /* create location configuration */
    ngx_http_response_body_merge_loc_conf    /* merge location configuration */
};


ngx_module_t  ngx_http_response_body_module = {
    NGX_MODULE_V1,
    &ngx_http_response_body_module_ctx,    /* module context */
    ngx_http_response_body_commands,       /* module directives */
    NGX_HTTP_MODULE,                       /* module type */
    NULL,                                  /* init master */
    NULL,                                  /* init module */
    NULL,                                  /* init process */
    NULL,                                  /* init thread */
    NULL,                                  /* exit thread */
    NULL,                                  /* exit process */
    NULL,                                  /* exit master */
    NGX_MODULE_V1_PADDING
};


static ngx_http_variable_t  ngx_http_upstream_vars[] = {

    { ngx_string("response_body"), NULL,
      ngx_http_response_body_variable, 0,
      NGX_HTTP_VAR_NOCACHEABLE, 0 },

    // ADDED FOR CUBE : embedded variables to access request and response headers
    { ngx_string("request_headers"), NULL,
        ngx_http_request_header_variable, 0,
        NGX_HTTP_VAR_NOCACHEABLE, 0 },

    { ngx_string("response_headers"), NULL,
        ngx_http_response_header_variable, 0,
        NGX_HTTP_VAR_NOCACHEABLE, 0 },

    { ngx_null_string, NULL, NULL, 0, 0, 0 }

};


static char *
ngx_conf_set_flag(ngx_conf_t *cf, ngx_command_t *cmd, void *conf)
{
    ngx_http_response_body_loc_conf_t  *blcf = conf;
    char                               *p = conf;
    ngx_flag_t                         *fp = (ngx_flag_t *) (p + cmd->offset);
    ngx_flag_t                          prev = *fp;

    *fp = NGX_CONF_UNSET;

    if (ngx_conf_set_flag_slot(cf, cmd, conf) != NGX_CONF_OK)
        return NGX_CONF_ERROR;

    if (prev != NGX_CONF_UNSET)
        *fp = ngx_max(prev, *fp);

    blcf->capture_body = *fp;

    return NGX_CONF_OK;
}


char *
ngx_conf_set_msec(ngx_conf_t *cf, ngx_command_t *cmd, void *conf)
{
    ngx_http_response_body_loc_conf_t  *blcf = conf;
    char                               *p = conf;
    ngx_msec_t                         *fp = (ngx_msec_t *) (p + cmd->offset);
    ngx_msec_t                          prev = *fp;

    *fp = NGX_CONF_UNSET_MSEC;

    if (ngx_conf_set_msec_slot(cf, cmd, conf) != NGX_CONF_OK)
        return NGX_CONF_ERROR;

    if (prev != NGX_CONF_UNSET_MSEC)
        *fp = ngx_min(prev, *fp);

    blcf->capture_body = 1;

    return NGX_CONF_OK;
}


static char *
ngx_conf_set_size(ngx_conf_t *cf, ngx_command_t *cmd, void *conf)
{
    char    *p = conf;
    size_t  *fp = (size_t *) (p + cmd->offset);
    size_t   prev = *fp;

    *fp = NGX_CONF_UNSET_SIZE;

    if (ngx_conf_set_size_slot(cf, cmd, conf) != NGX_CONF_OK)
        return NGX_CONF_ERROR;

    if (prev != NGX_CONF_UNSET_SIZE)
        *fp = ngx_max(prev, *fp);

    return NGX_CONF_OK;
}


static char *
ngx_conf_set_keyval(ngx_conf_t *cf, ngx_command_t *cmd, void *conf)
{
    ngx_http_response_body_loc_conf_t  *blcf = conf;

    if (ngx_conf_set_keyval_slot(cf, cmd, conf) != NGX_CONF_OK)
        return NGX_CONF_ERROR;

    blcf->capture_body = 1;

    return NGX_CONF_OK;
}


static ngx_int_t
ngx_http_response_body_add_variables(ngx_conf_t *cf)
{
    ngx_http_variable_t  *var, *v;

    for (v = ngx_http_upstream_vars; v->name.len; v++) {
        var = ngx_http_add_variable(cf, &v->name, v->flags);
        if (var == NULL) {
            return NGX_ERROR;
        }

        var->get_handler = v->get_handler;
        var->data = v->data;
    }

    return NGX_OK;
}

// ADDED FOR CUBE : taking common function out
static ngx_int_t transfer_buffer_to_variable(ngx_http_variable_value_t *v, ngx_buf_t *b) {
  if (b->start == NULL) {
      v->not_found = 1;
      return NGX_OK;
  }

  v->data = b->start;
  v->len = b->last - b->start;
  return NGX_OK;
}

static ngx_int_t
ngx_http_response_body_variable(ngx_http_request_t *r,
    ngx_http_variable_value_t *v, uintptr_t data)
{
    ngx_http_response_body_ctx_t *ctx;

    v->valid = 1;
    v->no_cacheable = 0;
    v->not_found = 0;

    ctx = ngx_http_get_module_ctx(r, ngx_http_response_body_module);
    if (ctx == NULL) {
        v->not_found = 1;
        return NGX_OK;
    }

    return transfer_buffer_to_variable(v, &ctx->buffer);
}


// ADDED FOR CUBE : function implementations
static ngx_int_t
ngx_http_request_header_variable(ngx_http_request_t *r,
    ngx_http_variable_value_t *v, uintptr_t data)
{
    ngx_http_response_body_ctx_t *ctx;

    v->valid = 1;
    v->no_cacheable = 0;
    v->not_found = 0;

    ctx = ngx_http_get_module_ctx(r, ngx_http_response_body_module);
    if (ctx == NULL) {
        v->not_found = 1;
        return NGX_OK;
    }

    return transfer_buffer_to_variable(v, &ctx->req_header_buffer);
}

static ngx_int_t
ngx_http_response_header_variable(ngx_http_request_t *r,
    ngx_http_variable_value_t *v, uintptr_t data)
{
    ngx_http_response_body_ctx_t *ctx;

    v->valid = 1;
    v->no_cacheable = 0;
    v->not_found = 0;

    ctx = ngx_http_get_module_ctx(r, ngx_http_response_body_module);
    if (ctx == NULL) {
        v->not_found = 1;
        return NGX_OK;
    }

    return transfer_buffer_to_variable(v, &ctx->resp_header_buffer);
}


static char *
ngx_http_response_body_request_var(ngx_conf_t *cf, ngx_command_t *cmd,
    void *conf)
{
    ngx_http_response_body_loc_conf_t *ulcf = conf;
    ngx_http_variable_t               *var;

    ulcf->capture_body_var = ((ngx_str_t *)cf->args->elts) [1];

    var = ngx_http_add_variable(cf, &ulcf->capture_body_var,
                                NGX_HTTP_VAR_NOCACHEABLE);
    if (var == NULL) {
        return NGX_CONF_ERROR;
    }

    var->get_handler = ngx_http_response_body_variable;
    var->data = 0;

    return NGX_CONF_OK;
}


static void *
ngx_http_response_body_create_loc_conf(ngx_conf_t *cf)
{
    ngx_http_response_body_loc_conf_t  *blcf;

    blcf = ngx_pcalloc(cf->pool, sizeof(ngx_http_response_body_loc_conf_t));

    if (blcf == NULL)
        return NULL;

    blcf->latency      = NGX_CONF_UNSET_MSEC;
    blcf->buffer_size  = NGX_CONF_UNSET_SIZE;
    blcf->header_buffer_size  = NGX_CONF_UNSET_SIZE;
    blcf->conditions   = ngx_array_create(cf->pool, 10, sizeof(ngx_keyval_t));
    blcf->cv           = ngx_array_create(cf->pool, 10,
        sizeof(ngx_http_complex_value_t));
    blcf->status_1xx   = NGX_CONF_UNSET;
    blcf->status_2xx   = NGX_CONF_UNSET;
    blcf->status_3xx   = NGX_CONF_UNSET;
    blcf->status_4xx   = NGX_CONF_UNSET;
    blcf->status_5xx   = NGX_CONF_UNSET;
    blcf->capture_body = NGX_CONF_UNSET;

    if (blcf->conditions == NULL || blcf->cv == NULL)
        return NULL;

    return blcf;
}


static ngx_int_t
ngx_array_merge(ngx_array_t *l, ngx_array_t *r)
{
    void  *p;

    if (r->nelts == 0)
        return NGX_OK;

    if (r->size != l->size)
        return NGX_ERROR;

    p = ngx_array_push_n(l, r->nelts);
    if (p == NULL)
        return NGX_ERROR;

    ngx_memcpy(p, r->elts, r->size * r->nelts);

    return NGX_OK;
}


static char *
ngx_http_response_body_merge_loc_conf(ngx_conf_t *cf, void *parent, void *child)
{
    ngx_http_response_body_loc_conf_t  *prev = parent;
    ngx_http_response_body_loc_conf_t  *conf = child;
    ngx_http_compile_complex_value_t    ccv;
    ngx_http_complex_value_t           *cv;
    ngx_uint_t                          j;
    ngx_keyval_t                       *kv;

    ngx_conf_merge_msec_value(conf->latency, prev->latency, (ngx_msec_int_t) 0);
    ngx_conf_merge_size_value(conf->buffer_size, prev->buffer_size,
                              (size_t) ngx_pagesize);
    ngx_conf_merge_size_value(conf->header_buffer_size, prev->header_buffer_size,
                                                        (size_t) ngx_pagesize);
    if (ngx_array_merge(conf->conditions, prev->conditions) == NGX_ERROR)
        return NGX_CONF_ERROR;
    ngx_conf_merge_value(conf->status_1xx, prev->status_1xx, 0);
    ngx_conf_merge_value(conf->status_2xx, prev->status_2xx, 0);
    ngx_conf_merge_value(conf->status_3xx, prev->status_3xx, 0);
    ngx_conf_merge_value(conf->status_4xx, prev->status_4xx, 0);
    ngx_conf_merge_value(conf->status_5xx, prev->status_5xx, 0);
    ngx_conf_merge_value(conf->capture_body, prev->capture_body, 0);

    cv = ngx_array_push_n(conf->cv, conf->conditions->nelts);
    if (cv == NULL)
        return NGX_CONF_ERROR;
    ngx_memzero(cv, conf->cv->size * conf->cv->nalloc);
    kv = conf->conditions->elts;

    for (j = 0; j < conf->cv->nelts; j++) {

        ngx_memzero(&ccv, sizeof(ccv));

        ccv.cf = cf;
        ccv.value = &kv[j].key;
        ccv.complex_value = &cv[j];
        ccv.zero = 0;

        if (ngx_http_compile_complex_value(&ccv) != NGX_OK) {

            ngx_conf_log_error(NGX_LOG_ERR, cf, 0,
                               "can't compile '%V'", &kv[j].key);
            return NGX_CONF_ERROR;
        }
    }

    return NGX_CONF_OK;
}


static ngx_int_t
ngx_http_response_body_init(ngx_conf_t *cf)
{
    ngx_http_handler_pt        *h;
    ngx_http_core_main_conf_t  *cmcf;

    cmcf = ngx_http_conf_get_module_main_conf(cf, ngx_http_core_module);

    h = ngx_array_push(&cmcf->phases[NGX_HTTP_LOG_PHASE].handlers);
    if (h == NULL)
        return NGX_ERROR;

    *h = ngx_http_response_body_log;

    ngx_http_next_header_filter = ngx_http_top_header_filter;
    ngx_http_top_header_filter = ngx_http_response_body_filter_header;

    ngx_http_next_body_filter = ngx_http_top_body_filter;
    ngx_http_top_body_filter = ngx_http_response_body_filter_body;

    if (ngx_http_next_header_filter == NULL)
        ngx_http_next_header_filter = ngx_http_next_header_filter_stub;

    if (ngx_http_next_body_filter == NULL)
        ngx_http_next_body_filter = ngx_http_next_body_filter_stub;

    return NGX_OK;
}


static ngx_int_t
ngx_http_response_body_set_ctx(ngx_http_request_t *r)
{
    ngx_http_response_body_loc_conf_t  *ulcf;
    ngx_http_response_body_ctx_t       *ctx;

    ulcf = ngx_http_get_module_loc_conf(r, ngx_http_response_body_module);

    if (!ulcf->capture_body)
        return NGX_DECLINED;

    ctx = ngx_pcalloc(r->pool, sizeof(ngx_http_response_body_ctx_t));
    if (ctx == NULL)
        return NGX_ERROR;

    ctx->blcf = ulcf;

    ngx_http_set_ctx(r, ctx, ngx_http_response_body_module);

    return NGX_OK;
}


static ngx_int_t
ngx_http_response_body_log(ngx_http_request_t *r)
{
    return NGX_OK;
}


static ngx_msec_t
ngx_http_response_body_request_time(ngx_http_request_t *r)
{
    ngx_time_t      *tp;
    ngx_msec_int_t   ms;

    tp = ngx_timeofday();

    ms = (ngx_msec_int_t)
             ((tp->sec - r->start_sec) * 1000 + (tp->msec - r->start_msec));

    return (ngx_msec_t) ngx_max(ms, 0);
}

// ADDED FOR CUBE : allocating memory buffer with a given size
static ngx_int_t allocate_buffer_if_already_not(ngx_buf_t *b, size_t buffer_size, ngx_http_request_t *r)
{
    if (b->start == NULL) {
        b->start = ngx_palloc(r->pool, buffer_size);
        if (b->start == NULL)
            return NGX_ERROR;
        ngx_log_debug1(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
                       "CUBE : Allocated memory of size %d for header" , buffer_size);
        b->end = b->start + buffer_size;
        b->pos = b->last = b->start;
    }
    return NGX_OK;
}

// ADDED FOR CUBE : copying headers to memory buffer
static ngx_int_t copy_headers_to_buffer(ngx_buf_t *b, ngx_list_part_t* part, ngx_http_request_t *r, ngx_buf_t *tmp_buf)
{
    ngx_table_elt_t   *header_elts;
    ngx_uint_t        i;

    header_elts = part->elts;
    b->last = ngx_cpymem(b->last, "[" , ngx_strlen("["));
    ngx_uint_t count = 0;
    size_t buf_len;

    for (i = 0; /* void */; i++) {
      if (i >= part->nelts) {
        if (part->next == NULL) {
            if (count > 0)
              b->last --;
            break;
        }
        part = part->next;
        header_elts = part->elts;
        i = 0;
      }

      count ++;
      b->last = ngx_cpymem(b->last, "{" , ngx_strlen("{"));
      b->last = ngx_cpymem(b->last, "\"" , ngx_strlen("\""));

      tmp_buf->start = ngx_palloc(r->pool, header_elts[i].key.len*2);
      if (tmp_buf->start == NULL) {
        ngx_log_error(NGX_LOG_WARN, r->connection->log, 0,
            "CUBE :: Could not allocate temporary buffer");
      }
      tmp_buf->end = tmp_buf->start + header_elts[i].key.len*2;
      tmp_buf->pos = tmp_buf->last = tmp_buf->start;

      buf_len = escape_special_char(tmp_buf, header_elts[i].key.data,
        header_elts[i].key.len);

      b->last = ngx_cpymem(b->last, tmp_buf->pos , buf_len);

      ngx_pfree(r->pool, tmp_buf);
      b->last = ngx_cpymem(b->last, "\"" , ngx_strlen("\""));
      b->last = ngx_cpymem(b->last, ":" , ngx_strlen(":"));
      b->last = ngx_cpymem(b->last, "\"" , ngx_strlen("\""));

      tmp_buf->start  = ngx_palloc(r->pool, header_elts[i].value.len*2);
      if (tmp_buf->start == NULL) {
        ngx_log_error(NGX_LOG_WARN, r->connection->log, 0,
            "CUBE :: Could not allocate temporary buffer");
      }
      tmp_buf->end = tmp_buf->start + header_elts[i].value.len*2;
      tmp_buf->pos = tmp_buf->last = tmp_buf->start;

      buf_len = escape_special_char(tmp_buf, header_elts[i].value.data,
        header_elts[i].value.len);

      b->last = ngx_cpymem(b->last, tmp_buf->pos , buf_len);
      b->last = ngx_cpymem(b->last, "\"" , ngx_strlen("\""));
      b->last = ngx_cpymem(b->last, "}" , ngx_strlen("}"));
      b->last = ngx_cpymem(b->last, "," , ngx_strlen(","));

      ngx_pfree(r->pool, tmp_buf);
    }

    b->last = ngx_cpymem(b->last, "]" , ngx_strlen("]"));
    return NGX_OK;

}

static size_t escape_special_char(ngx_buf_t* target_buf, u_char* source,
    size_t source_len) {
    ngx_uint_t j;
    size_t escaped_str_len = 0;
    for (j = 0; j < source_len; j++) {
      switch(*source) {
        case '\b':
          target_buf->last = ngx_cpymem(target_buf->last, "\\b", 2);
          escaped_str_len += 2;
          break;
        case '\f':
          target_buf->last = ngx_cpymem(target_buf->last, "\\f", 2);
          escaped_str_len += 2;
          break;
        case '\n':
          target_buf->last = ngx_cpymem(target_buf->last, "\\n", 2);
          escaped_str_len += 2;
          break;
        case '\r':
          target_buf->last = ngx_cpymem(target_buf->last, "\\r", 2);
          escaped_str_len += 2;
          break;
        case '\t':
          target_buf->last = ngx_cpymem(target_buf->last, "\\t", 2);
          escaped_str_len += 2;
          break;
        case '"':
        case '\\':
        case '/':
          target_buf->last = ngx_cpymem(target_buf->last, "\\", 1);
          escaped_str_len += 1;
        default:
          target_buf->last = ngx_cpymem(target_buf->last, source, 1);
          escaped_str_len += 1;
          break;
      }
      source ++;
    }
    return escaped_str_len;
}

static size_t estimate_copy_size(ngx_list_part_t* part) {
    size_t header_size = 0;
    ngx_uint_t count = 0;
    ngx_table_elt_t   *header_elts;
    ngx_uint_t        i;

    header_size += ngx_strlen("[]");
    header_elts = part->elts;

    for (i =0; /* void */;i++) {
      if (i >= part->nelts) {
        if (part->next == NULL) {
          if (count > 0)
            header_size--;
          break;
        }
        part = part->next;
        header_elts = part->elts;
        i = 0;
      }
      count ++;
      // key and value * 2 to take into account escaping
      header_size += header_elts[i].key.len*2 + header_elts[i].value.len*2
        + 4*ngx_strlen("\"") + ngx_strlen(",") + ngx_strlen(":") + ngx_strlen("{}");
    }

    return header_size;
}

static ngx_int_t
ngx_http_response_body_filter_header(ngx_http_request_t *r)
{
    ngx_http_response_body_ctx_t  *ctx;
    ngx_uint_t                     j;
    ngx_http_complex_value_t      *cv;
    ngx_str_t                      value;
    ngx_keyval_t                  *kv;
    size_t                    header_size;
    // this call if successful , will set the context properly
    // and further response body capture will happen
    switch (ngx_http_response_body_set_ctx(r)) {
        case NGX_OK:
            break;

        case NGX_DECLINED:
            ngx_log_debug0(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
                   "CUBE :: Setting context declined because capture body false");
            return ngx_http_next_header_filter(r);

        case NGX_ERROR:
            ngx_log_error(NGX_LOG_WARN, r->connection->log, 0,
                "CUBE :: ngx_http_response_body_filter_header: no memory");
            return NGX_HTTP_INTERNAL_SERVER_ERROR;

        default:
            return ngx_http_next_header_filter(r);
    }

    ctx = ngx_http_get_module_ctx(r, ngx_http_response_body_module);

    // ADDED FOR CUBE :: Logic for capturing request and response headers
    if (ctx->blcf->capture_body == 1) {

      switch(allocate_buffer_if_already_not(&ctx->req_header_buffer, ctx->blcf->header_buffer_size,r)) {
          case NGX_ERROR:
            ngx_log_error(NGX_LOG_WARN, r->connection->log, 0,
                "CUBE :: Unable to allocate memory for req header buffer");
            return ngx_http_next_header_filter(r);
          default:
            // CUBE : currently the estimate_copy_size function is external to
            // copy_headers_to_buffer function, although at a later stage it might
            // be added to the copy function itself, to avoid the second traversal,
            // in that case the if condition can be removed
            header_size = estimate_copy_size(&r->headers_in.headers.part);
            if (header_size <= ctx->blcf->header_buffer_size) {
              copy_headers_to_buffer(&ctx->req_header_buffer, &r->headers_in.headers.part,r,&ctx->header_value_buffer);
              ngx_log_debug0(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
                         "CUBE :: Successfully captured request header in context variable");
            } else {
              ngx_log_debug1(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
                         "CUBE :: Buffer size not enough to hold request headers :: %i" , header_size );
            }
      }

      switch(allocate_buffer_if_already_not(&ctx->resp_header_buffer, ctx->blcf->header_buffer_size,r)) {
          case NGX_ERROR:
            ngx_log_error(NGX_LOG_WARN, r->connection->log, 0,
                "CUBE :: unable to allocate memory for req header buffer");
            return ngx_http_next_header_filter(r);
          default:
            header_size = estimate_copy_size(&r->headers_out.headers.part);
            if (header_size <= ctx->blcf->header_buffer_size) {
              copy_headers_to_buffer(&ctx->resp_header_buffer, &r->headers_out.headers.part,r,&ctx->header_value_buffer);
              ngx_log_debug0(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
                         "CUBE :: Successfully captured response headers in context variable");
            } else {
              ngx_log_debug1(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
                         "CUBE :: Buffer size not enough to hold response headers :: %i" , header_size);
            }

      }

    }
    // ADDED FOR CUBE :: END

    if (r->headers_out.status < 200) {

       if (ctx->blcf->status_1xx)
           return ngx_http_next_header_filter(r);

    } else if (r->headers_out.status < 300) {

        if (ctx->blcf->status_2xx)
           return ngx_http_next_header_filter(r);

    } else if (r->headers_out.status < 400) {

        if (ctx->blcf->status_3xx)
           return ngx_http_next_header_filter(r);

    } else if (r->headers_out.status < 500) {

        if (ctx->blcf->status_4xx)
           return ngx_http_next_header_filter(r);

    } else {

        if (ctx->blcf->status_5xx)
           return ngx_http_next_header_filter(r);

    }

    if (ctx->blcf->latency != 0
        && ctx->blcf->latency <= ngx_http_response_body_request_time(r))
        return ngx_http_next_header_filter(r);

    cv = ctx->blcf->cv->elts;
    kv = ctx->blcf->conditions->elts;

    for (j = 0; j < ctx->blcf->cv->nelts; ++j) {

        if (ngx_http_complex_value(r, &cv[j], &value) != NGX_OK)
            continue;

        if (value.len == 0)
            continue;

        if (kv[j].value.len == 0
            || (kv[j].value.len == 1 && kv[j].value.data[0] == '*'))
            return ngx_http_next_header_filter(r);

        if (kv[j].value.len == value.len
            && ngx_strncasecmp(value.data, kv[j].value.data, value.len) == 0)
            return ngx_http_next_header_filter(r);
    }
    // This will set the context back to null and no response capture will happen
    // We will reach this branch only when some of the conditions are not satisfied
    ngx_http_set_ctx(r, NULL, ngx_http_response_body_module);
    return ngx_http_next_header_filter(r);
}


static ngx_int_t
ngx_http_response_body_filter_body(ngx_http_request_t *r, ngx_chain_t *in)
{
    ngx_http_response_body_ctx_t       *ctx;
    ngx_chain_t                        *cl;
    ngx_buf_t                          *b;
    ngx_http_response_body_loc_conf_t  *conf;
    size_t                              len;
    ssize_t                             rest;

    ctx = ngx_http_get_module_ctx(r, ngx_http_response_body_module);
    if (ctx == NULL)
        return ngx_http_next_body_filter(r, in);

    conf = ngx_http_get_module_loc_conf(r, ngx_http_response_body_module);

    b = &ctx->buffer;

    if (b->start == NULL) {

        b->start = ngx_palloc(r->pool, conf->buffer_size);
        if (b->start == NULL)
            return NGX_ERROR;
       ngx_log_debug1(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
                       "CUBE : Allocated buffer of size %d for response body" , conf->buffer_size);
        b->end = b->start + conf->buffer_size;
        b->pos = b->last = b->start;
    }

    for (cl = in; cl; cl = cl->next) {

        rest = b->end - b->last;
        if (rest == 0)
            break;

        if (!ngx_buf_in_memory(cl->buf))
            continue;

        len = cl->buf->last - cl->buf->pos;

        if (len == 0)
            continue;

        if (len > (size_t) rest)
            /* we truncate the exceeding part of the response body */
            len = rest;

        b->last = ngx_copy(b->last, cl->buf->pos, len);
    }

    ngx_log_debug0(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
               "CUBE : Successfully captured response body in context variable");
    return ngx_http_next_body_filter(r, in);
}
