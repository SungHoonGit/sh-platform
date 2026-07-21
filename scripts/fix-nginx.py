#!/usr/bin/env python3
"""nginx 설정 자동 수정: SPA fallback + scraper-ui location 블록 추가"""
import subprocess, sys, os, tempfile

CONFIG = '/etc/nginx/sites-available/sh-platform'

def read_config():
    r = subprocess.run(['sudo', 'cat', CONFIG], capture_output=True, text=True)
    r.check_returncode()
    return r.stdout

def write_config(content):
    with tempfile.NamedTemporaryFile(mode='w', suffix='.tmp', delete=False) as f:
        f.write(content)
        tmp = f.name
    subprocess.run(['sudo', 'cp', tmp, CONFIG], check=True)
    os.unlink(tmp)

def main():
    content = read_config()
    original = content

    content = content.replace(
        'try_files $uri $uri/;',
        'try_files $uri $uri/ /index.html;'
    )

    if 'location /scraper-ui' not in content:
        block = '''
    # Scraper frontend
    location /scraper-ui {
        alias /home/ubuntu/sh-platform/modules/scraper/frontend/dist;
        try_files $uri $uri/ /scraper-ui/index.html;
    }'''
        content = content.rstrip()
        if content.endswith('}'):
            i = content.rfind('\n}')
            if i > 0:
                content = content[:i] + block + '\n}'

    if content == original:
        print("NO_CHANGES")
        return

    write_config(content)

    r = subprocess.run(['sudo', 'nginx', '-t'], capture_output=True, text=True)
    if r.returncode != 0:
        print(f"NGINX_TEST_FAIL: {r.stderr}")
        sys.exit(1)

    subprocess.run(['sudo', 'systemctl', 'reload', 'nginx'], check=True)
    print("NGINX_OK")

if __name__ == '__main__':
    main()
