import os
from playwright.sync_api import sync_playwright

def main():
    cwd = os.getcwd()
    preview_path = f"file://{cwd}/UI_PREVIEW.html"
    print(f"Opening: {preview_path}")

    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        page.goto(preview_path)

        # Wait for Alpine
        page.wait_for_timeout(1000)

        # Status Tab
        page.screenshot(path="verification_status.png")
        print("Captured verification_status.png")

        # Device Tab (simulate click)
        # The tabs are buttons with text inside spans.
        # Let's target by text content "device" inside the bottom nav
        # But wait, the text is inside a span with x-text="item".
        # Let's use a selector based on the tab variable or text.

        # Click the button that sets tab = 'device'
        # In UI_PREVIEW.html: <button @click="tab = item" ...> ... <span ... x-text="item">device</span> ... </button>
        # We can just use text="device" (case insensitive usually in playwright locators if configured, or exact).
        # But the text is uppercase in the span via CSS? No, "text-[10px] uppercase". The DOM text is "device".

        page.get_by_text("device", exact=True).click()
        page.wait_for_timeout(500)
        page.screenshot(path="verification_device.png")
        print("Captured verification_device.png")

        # IDs Tab
        page.get_by_text("ids", exact=True).click()
        page.wait_for_timeout(500)
        page.screenshot(path="verification_ids.png")
        print("Captured verification_ids.png")

        # Advanced Tab
        page.get_by_text("advanced", exact=True).click()
        page.wait_for_timeout(500)
        page.screenshot(path="verification_advanced.png")
        print("Captured verification_advanced.png")

        browser.close()

if __name__ == "__main__":
    main()
