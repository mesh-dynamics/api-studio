from jinja2 import Template

with open('nginx_record.conf.j2', 'r') as file:
    data = file.read()
    t = Template(data)
    rendered = t.render(customer_id='CubeCorp',instance_id='test-dc',app_name='MovieInfo',run_type='Record',mock_host='http://blah')
    with open('nginx_record.conf' , 'w') as conf_file:
        conf_file.write(rendered)

with open('nginx_mock.conf.j2', 'r') as file:
    data = file.read()
    t = Template(data)
    rendered = t.render(customer_id='CubeCorp',instance_id='test-dc',app_name='MovieInfo',run_type='Replay',mock_host='staging-sm.dev.cubecorp.io')
    with open('nginx_mock.conf' , 'w') as conf_file:
        conf_file.write(rendered)

with open('fluentd.conf.j2', 'r') as file:
    data = file.read()
    t = Template(data)
    rendered = t.render(path="/var/log/nginx/cube.http.access.log",pos_file="/home/ec2-user/cube.http.access.log.pos",endpoint="http://staging-sm.dev.cubecorp.io/cs/rrbatch",chunk_limit_records="1")
    with open('fluentd.conf' , 'w') as conf_file:
        conf_file.write(rendered)
