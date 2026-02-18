from playwright.sync_api import sync_playwright

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()

        # Open the local file (absolute path)
        page.goto("file:///app/UI_PREVIEW.html")

        # Wait for Alpine to initialize
        page.wait_for_timeout(1000)

        # Take screenshot of Dashboard (Status Tab)
        page.screenshot(path="/home/jules/verification/preview_status.png", full_page=True)
        print("Status tab screenshot taken.")

        # Click "Device" tab (using role to be specific)
        page.get_by_role("button", name="device").click()
        page.wait_for_timeout(500)

        # Take screenshot of Device Tab
        page.screenshot(path="/home/jules/verification/preview_device.png", full_page=True)
        print("Device tab screenshot taken.")

        # Click "IDs" tab
        page.get_by_role("button", name="ids").click()
        page.wait_for_timeout(500)

        # Take screenshot of IDs Tab
        page.screenshot(path="/home/jules/verification/preview_ids.png", full_page=True)
        print("IDs tab screenshot taken.")

        browser.close()

if __name__ == "__main__":
    main()