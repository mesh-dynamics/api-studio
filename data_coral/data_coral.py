from jinja2 import Template

def render_template(in_file, params, out_file):
    with open(in_file, 'r') as file:
        data = file.read()
        t = Template(data)
        rendered = t.render(**params)
        with open(out_file, 'w') as conf_file:
            conf_file.write(rendered)

render_template('nginx_record.conf.j2', dict(customer_id='CubeCorp', instance_id='test-dc', app_name='MovieInfo',run_type='Record',mock_host='http://blah') , 'nginx_record.conf')
render_template('nginx_mock.conf.j2', dict(customer_id='CubeCorp',instance_id='test-dc',app_name='MovieInfo',run_type='Replay',mock_host='staging-sm.dev.cubecorp.io') , 'nginx_mock.conf')
render_template('fluentd.conf.j2', dict(path="/var/log/nginx/cube.http.access.log",pos_file="/home/ec2-user/cube.http.access.log.pos",endpoint="http://staging-sm.dev.cubecorp.io/cs/rrbatch",chunk_limit_records="2"), 'fluentd.conf')
