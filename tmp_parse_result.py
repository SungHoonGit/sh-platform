import json, sys
d = json.load(sys.stdin)
dd = d['data']
print('total=%d sr=%d jk=%d rem=%d t=%d' % (dd['total'], dd['siteCounts'].get('saramin',0), dd['siteCounts'].get('jobkorea',0), dd['siteCounts'].get('remember',0), dd['searchTime']))
