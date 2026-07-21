#!/usr/bin/env python3
"""nginx 설정 자동 수정: SPA fallback + scraper-ui location 블록 추가"""
import re, subprocess, sys, os, tempfile

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

    block = '''
    # Scraper frontend
    location /scraper-ui {
        alias /home/ubuntu/sh-platform/modules/scraper/frontend/dist;
        try_files $uri $uri/ /scraper-ui/index.html;
    }'''

    # Remove any existing scraper-ui block (was being added to wrong server block)
    content = re.sub(
        r'^\s+# Scraper frontend\n\s+location /scraper-ui \{[^}]+}\s*\n?',
        '', content, count=1, flags=re.MULTILINE
    )

    # Add to HTTPS server block (first server block, closes before }\n\nserver {)
    m = re.search(r'\n}\n\nserver \{', content)
    if m:
        insert_at = m.start() + 1  # position of '}'
        content = content[:insert_at] + block + '\n' + content[insert_at:]

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
