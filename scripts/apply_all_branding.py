import os
import sys
import re
import glob

# Allow path to be passed as argument or default to local path / CI path
if len(sys.argv) > 1:
    TG_PATH = sys.argv[1]
elif os.path.exists("telegram/TMessagesProj"):
    TG_PATH = "telegram"
else:
    TG_PATH = "/home/nawmbad/.gemini/antigravity-ide/scratch/telegram-real-fork"

print(f"=== Applying branding to: {TG_PATH} ===")

# 1. Patch all values-*/strings.xml
print("=== 1. Patching all strings.xml files ===")
strings_files = glob.glob(f"{TG_PATH}/TMessagesProj/src/main/res/values*/strings.xml")

for sf in strings_files:
    with open(sf, "r", encoding="utf-8") as f:
        content = f.read()

    orig_content = content

    # AppName & Page1Title
    content = re.sub(r'(<string name="AppName">)[^<]+(</string>)', r'\g<1>Nawmagram\2', content)
    content = re.sub(r'(<string name="AppNameBeta">)[^<]+(</string>)', r'\g<1>Nawmagram Beta\2', content)
    content = re.sub(r'(<string name="Page1Title">)[^<]+(</string>)', r'\g<1>Nawmagram\2', content)

    # Specific strings where "Telegram" is mentioned
    keys_to_replace = [
        "Page2Message", "Page3Message", "Page4Message", "Page5Message", "Page6Message",
        "SentAppCode", "SentAppCodeWithPhone", "SentAppCodeTitle", "SentCallCode",
        "CallText", "CancelAccountResetInfo2", "OtherLoginCode", "DidNotGetTheCodeInfo",
        "RecoveryEmailSubtitle", "EnabledPasswordText", "AddEmailSubtitle", "NoChats",
        "YourEmailSkipWarningText", "ResetMyAccountText", "ResetMyAccountWarningText"
    ]

    for key in keys_to_replace:
        pattern = rf'(<string name="{key}">)(.*?)(</string>)'
        def repl(m):
            header = m.group(1)
            body = m.group(2)
            tail = m.group(3)
            body = body.replace("Telegram", "Nawmagram")
            body = body.replace("телеграм", "Nawmagram")
            body = body.replace("Телеграм", "Nawmagram")
            return f"{header}{body}{tail}"
        content = re.sub(pattern, repl, content, flags=re.DOTALL)

    if content != orig_content:
        with open(sf, "w", encoding="utf-8") as f:
            f.write(content)
        rel_path = os.path.relpath(sf, TG_PATH)
        print(f"Patched: {rel_path}")

# 2. Patch IntroActivity.java (remove ImageSpan logo that overlays Nawmagram text, keep logoDrawable non-null)
print("\n=== 2. Patching IntroActivity.java ===")
intro_path = f"{TG_PATH}/TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java"
if os.path.exists(intro_path):
    with open(intro_path, "r", encoding="utf-8") as f:
        intro_code = f.read()

    target_intro = """        logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo).mutate();
        logoDrawable.setBounds(0, dp(8.666f), dp(115), dp(35));
        SpannableStringBuilder ssb = new SpannableStringBuilder(LocaleController.getString(R.string.Page1Title));
        ssb.setSpan(new ImageSpan(logoDrawable), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titles[0] = ssb;"""

    replacement_intro = """        // Nawmagram: Initialize logoDrawable so updateColors() doesn't NPE, but display text title
        logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo).mutate();
        titles[0] = LocaleController.getString(R.string.Page1Title);"""

    if target_intro in intro_code:
        intro_code = intro_code.replace(target_intro, replacement_intro)
        print("Successfully patched titles[0] in IntroActivity.java")
    elif "titles[0] = LocaleController.getString(R.string.Page1Title);" in intro_code and "logoDrawable = context.getResources()" not in intro_code:
        # Was previously patched without logoDrawable init - fix it!
        intro_code = intro_code.replace(
            "titles[0] = LocaleController.getString(R.string.Page1Title);",
            "logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo).mutate();\n        titles[0] = LocaleController.getString(R.string.Page1Title);"
        )
        print("Restored logoDrawable init in IntroActivity.java")

    # Add null check in updateColors
    intro_code = intro_code.replace(
        "logoDrawable.setColorFilter(",
        "if (logoDrawable != null) logoDrawable.setColorFilter("
    )

    with open(intro_path, "w", encoding="utf-8") as f:
        f.write(intro_code)
    print("IntroActivity.java patched with NullPointerException protection!")

# 3. Patch DialogsActivity.java (remove ImageSpan logo in chat list ActionBar, keep logoDrawable non-null)
print("\n=== 3. Patching DialogsActivity.java ===")
dialogs_path = f"{TG_PATH}/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java"
if os.path.exists(dialogs_path):
    with open(dialogs_path, "r", encoding="utf-8") as f:
        dialogs_code = f.read()

    target_dialogs = """                logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo_2).mutate();
                logoDrawable.setBounds(0, dp(2), logoDrawable.getIntrinsicWidth(), dp(2) + logoDrawable.getIntrinsicHeight());
                logoDrawable.setColorFilter(getThemedColor(Theme.key_telegram_color_dialogsLogo), PorterDuff.Mode.MULTIPLY);
                SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.AppName));
                ssb.setSpan(new ImageSpan(logoDrawable), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                actionBar.setTitle(ssb, statusDrawable);"""

    replacement_dialogs = """                // Nawmagram: Display text app title instead of Telegram vector logo
                logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo_2).mutate();
                SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.AppName));
                actionBar.setTitle(ssb, statusDrawable);"""

    if target_dialogs in dialogs_code:
        dialogs_code = dialogs_code.replace(target_dialogs, replacement_dialogs)
        with open(dialogs_path, "w", encoding="utf-8") as f:
            f.write(dialogs_code)
        print("Successfully patched DialogsActivity.java (removed vector logo in header)")

# 4. Patch DialogStoriesCell.java
print("\n=== 4. Patching DialogStoriesCell.java ===")
stories_path = f"{TG_PATH}/TMessagesProj/src/main/java/org/telegram/ui/Stories/DialogStoriesCell.java"
if os.path.exists(stories_path):
    with open(stories_path, "r", encoding="utf-8") as f:
        stories_code = f.read()

    target_stories = "telegramLogoView.setImageResource(R.drawable.telegram_logo_2);"
    replacement_stories = "telegramLogoView.setVisibility(View.GONE); // Nawmagram: hide vector logo"
    if target_stories in stories_code:
        stories_code = stories_code.replace(target_stories, replacement_stories)
        with open(stories_path, "w", encoding="utf-8") as f:
            f.write(stories_code)
        print("Successfully patched DialogStoriesCell.java")

print("\n=== All visual branding patches applied successfully! ===")
