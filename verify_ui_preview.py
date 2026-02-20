import os
from playwright.sync_api import sync_playwright

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1000, 'height': 1000}) # Large viewport to see the whole device frame

        cwd = os.getcwd()
        url = f"file://{cwd}/UI_PREVIEW.html"
        print(f"Loading {url}")
        page.goto(url)

        # Wait for Alpine to initialize and animation
        page.wait_for_timeout(1000)

        # Screenshot Status Tab
        page.screenshot(path="preview_status.png")
        print("Captured Status Tab")

        # Click Device Tab
        page.click("text=Device")
        page.wait_for_timeout(500)
        page.screenshot(path="preview_device.png")
        print("Captured Device Tab")

        # Click Network Tab
        page.click("text=Network")
        page.wait_for_timeout(500)
        page.screenshot(path="preview_network.png")
        print("Captured Network Tab")

        # Click IDs Tab (Check Validation Badges)
        page.click("text=IDs")
        page.wait_for_timeout(500)
        page.screenshot(path="preview_ids.png")
        print("Captured IDs Tab")

        # Click Location Tab
        page.click("text=Place")
        page.wait_for_timeout(500)
        page.screenshot(path="preview_location.png")
        print("Captured Location Tab")

        # Click Advanced Tab
        page.click("text=Adv")
        page.wait_for_timeout(500)
        page.screenshot(path="preview_advanced.png")
        print("Captured Advanced Tab")

        browser.close()

if __name__ == "__main__":
    run()
