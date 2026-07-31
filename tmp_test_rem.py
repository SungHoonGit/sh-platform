import json, subprocess, time
for i in range(3):
    r = subprocess.check_output(['curl', '-s', '--max-time', '45', '-X', 'POST',
        'http://localhost:8081/search',
        '-H', 'Content-Type: application/json',
        '-d@/tmp/rq_all_pscp.json'])
    d = json.loads(r)['data']
    print(str(i+1)+': total='+str(d['total'])+' rem='+str(d['siteCounts'].get('remember',0))+' jk='+str(d['siteCounts'].get('jobkorea',0))+' sr='+str(d['siteCounts'].get('saramin',0)))
    time.sleep(2)
