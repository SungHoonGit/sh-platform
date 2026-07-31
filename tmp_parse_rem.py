import json
for f, n in [('/tmp/res_a.json','a'),('/tmp/res_b.json','b'),('/tmp/res_c.json','c')]:
    d = json.load(open(f))
    dd = d['data']
    print('%s: total=%d rem=%d jk=%d sr=%d' % (n, dd['total'], dd['siteCounts'].get('remember',0), dd['siteCounts'].get('jobkorea',0), dd['siteCounts'].get('saramin',0)))
