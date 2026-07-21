#!/usr/bin/env python3
"""nginx 설정 자동 수정: SPA fallback + scraper-ui location 블록 추가"""

import re
import subprocess
import sys

CONFIG = '/etc/nginx/sites-available/sh-platform'

def main():
    with open(CONFIG) as f:
        content = f.read()
        original = content

    # 1. try_files에 /index.html fallback 추가
    content = content.replace(
        'try_files $uri $uri/;',
        'try_files $uri $uri/ /index.html;'
    )

    # 2. /scraper-ui location 블록 추가 (없는 경우에만)
    if 'location /scraper-ui' not in content:
        scraper_block = '''
    # Scraper frontend
    location /scraper-ui {
        alias /home/ubuntu/sh-platform/modules/scraper/frontend/dist;
        try_files $uri $uri/ /scraper-ui/index.html;
    }'''
        # server 블록의 마지막 } 전에 삽입
        content = content.rstrip()
        if content.endswith('}'):
            # 마지막 }를 찾아서 그 앞에 삽입
            last_brace = content.rfind('\n}')
            if last_brace > 0:
                content = content[:last_brace] + scraper_block + '\n}'

    if content == original:
        print("NO_CHANGES")
        return

    with open(CONFIG, 'w') as f:
        f.write(content)

    # 3. 테스트 및 리로드
    result = subprocess.run(['sudo', 'nginx', '-t'], capture_output=True, text=True)
    if result.returncode != 0:
        print(f"NGINX_TEST_FAIL: {result.stderr}")
        sys.exit(1)

    subprocess.run(['sudo', 'systemctl', 'reload', 'nginx'], check=True)
    print("NGINX_OK")

if __name__ == '__main__':
    main()
