import os
from playwright.sync_api import sync_playwright

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 400, 'height': 800})

        cwd = os.getcwd()
        url = f"file://{cwd}/UI_PREVIEW.html"
        print(f"Loading {url}")
        page.goto(url)

        # Wait for Alpine
        page.wait_for_timeout(2000)

        # Click IDs Tab
        # Using a more robust selector if text fails, but text should work now that file is correct
        try:
            page.click("text=IDs")
        except:
             print("Text selector failed, trying nav item index")
             # IDs is the 4th item (index 3 zero-based, or child 4)
             # But let's just try to screenshot main first
             pass

        page.wait_for_timeout(1000)
        page.screenshot(path="preview_final.png")
        print("Captured Final Preview")

        browser.close()

if __name__ == "__main__":
    run()
