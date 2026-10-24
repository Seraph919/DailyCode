/**
 * DailyCode Discord Bot - Landing Page & Permissions Logic
 */

const BOT_CLIENT_ID = "1559164347085885511";

// Discord Permission Bit Flags (BigInt to handle 64-bit integer values safely)
const PERMISSIONS = {
  // General
  VIEW_CHANNEL: 1024n,               // 0x400
  READ_MESSAGE_HISTORY: 65536n,      // 0x10000
  MANAGE_CHANNELS: 16n,              // 0x10
  MANAGE_ROLES: 268435456n,          // 0x10000000
  ADMINISTRATOR: 8n,                 // 0x8

  // Text / Messaging
  SEND_MESSAGES: 2048n,              // 0x800
  SEND_MESSAGES_IN_THREADS: 274877906944n, // 0x4000000000
  EMBED_LINKS: 16384n,               // 0x4000
  ATTACH_FILES: 32768n,              // 0x8000
  ADD_REACTIONS: 64n,                // 0x40
  USE_EXTERNAL_EMOJIS: 262144n,      // 0x40000
  MENTION_EVERYONE: 131072n,         // 0x20000
};

// Permission Presets
const PRESETS = {
  recommended: [
    "VIEW_CHANNEL",
    "READ_MESSAGE_HISTORY",
    "SEND_MESSAGES",
    "SEND_MESSAGES_IN_THREADS",
    "EMBED_LINKS",
    "ATTACH_FILES",
    "ADD_REACTIONS",
    "USE_EXTERNAL_EMOJIS",
    "MENTION_EVERYONE"
  ],
  minimal: [
    "VIEW_CHANNEL",
    "READ_MESSAGE_HISTORY",
    "SEND_MESSAGES",
    "EMBED_LINKS"
  ],
  admin: [
    "ADMINISTRATOR"
  ]
};

document.addEventListener("DOMContentLoaded", () => {
  initPermissionsCalculator();
  initCommandFilters();
  initCopyButtons();
  initFaqAccordion();
  initMobileMenu();
  initMockupInteractivity();
});

/**
 * Initializes the Discord permissions calculator
 */
function initPermissionsCalculator() {
  const checkboxes = document.querySelectorAll(".perm-check");
  const permValueEl = document.getElementById("perm-integer");
  const inviteBtn = document.getElementById("custom-invite-btn");
  const copyInviteBtn = document.getElementById("copy-invite-btn");
  const presetBtns = document.querySelectorAll(".preset-btn");

  if (!checkboxes.length || !permValueEl) return;

  function calculatePermissions() {
    let bitmask = 0n;
    checkboxes.forEach((cb) => {
      if (cb.checked && PERMISSIONS[cb.value]) {
        bitmask |= PERMISSIONS[cb.value];
      }
    });

    const bitmaskStr = bitmask.toString();
    permValueEl.textContent = bitmaskStr;

    const oauthUrl = `https://discord.com/oauth2/authorize?client_id=${BOT_CLIENT_ID}&permissions=${bitmaskStr}&scope=bot%20applications.commands`;

    if (inviteBtn) {
      inviteBtn.href = oauthUrl;
    }

    // Also update any primary CTA add-bot buttons that don't have hardcoded links
    document.querySelectorAll(".dynamic-invite-link").forEach((link) => {
      link.href = oauthUrl;
    });

    return oauthUrl;
  }

  // Checkbox change listener
  checkboxes.forEach((cb) => {
    cb.addEventListener("change", () => {
      // Clear active preset highlight if modified manually
      presetBtns.forEach((b) => b.classList.remove("active"));
      calculatePermissions();
    });
  });

  // Preset button clicks
  presetBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      const presetName = btn.dataset.preset;
      const targetPerms = PRESETS[presetName] || [];

      presetBtns.forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");

      checkboxes.forEach((cb) => {
        cb.checked = targetPerms.includes(cb.value);
      });

      calculatePermissions();
    });
  });

  // Copy customized invite URL
  if (copyInviteBtn) {
    copyInviteBtn.addEventListener("click", () => {
      const url = calculatePermissions();
      navigator.clipboard.writeText(url).then(() => {
        showToast("Invite URL copied to clipboard!");
      }).catch(() => {
        // Fallback
        showToast("Link ready: " + url.substring(0, 35) + "...");
      });
    });
  }

  // Initial calculation
  calculatePermissions();
}

/**
 * Slash commands category filtering
 */
function initCommandFilters() {
  const filterBtns = document.querySelectorAll(".filter-btn");
  const commandCards = document.querySelectorAll(".command-card");

  filterBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      filterBtns.forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");

      const category = btn.dataset.category;

      commandCards.forEach((card) => {
        if (category === "all" || card.dataset.category === category) {
          card.style.display = "flex";
        } else {
          card.style.display = "none";
        }
      });
    });
  });
}

/**
 * Click-to-copy handler (Brutalist)
 */
function initCopyButtons() {
  document.querySelectorAll(".copy-btn").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      const textToCopy = btn.dataset.copy;
      if (!textToCopy) return;

      navigator.clipboard.writeText(textToCopy).then(() => {
        showToast(`COPIED: "${textToCopy}"`);
        const originalContent = btn.innerHTML;
        btn.innerHTML = `<span>[✓ COPIED]</span>`;
        btn.style.background = "#FF8800";
        btn.style.color = "#000000";
        setTimeout(() => {
          btn.innerHTML = originalContent;
          btn.style.background = "";
          btn.style.color = "";
        }, 1500);
      });
    });
  });
}

/**
 * FAQ accordion toggle (Brutalist [+] / [-])
 */
function initFaqAccordion() {
  const faqItems = document.querySelectorAll(".faq-item");

  faqItems.forEach((item) => {
    const questionBtn = item.querySelector(".faq-question");
    const answer = item.querySelector(".faq-answer");
    const icon = item.querySelector(".faq-toggle-icon");

    if (!questionBtn || !answer) return;

    questionBtn.addEventListener("click", () => {
      const isOpen = item.classList.contains("active");

      // Close all other items
      faqItems.forEach((other) => {
        if (other !== item) {
          other.classList.remove("active");
          const otherAns = other.querySelector(".faq-answer");
          const otherIcon = other.querySelector(".faq-toggle-icon");
          if (otherAns) otherAns.style.maxHeight = null;
          if (otherIcon) otherIcon.textContent = "[+]";
        }
      });

      // Toggle current
      if (isOpen) {
        item.classList.remove("active");
        answer.style.maxHeight = null;
        if (icon) icon.textContent = "[+]";
      } else {
        item.classList.add("active");
        answer.style.maxHeight = answer.scrollHeight + 30 + "px";
        if (icon) icon.textContent = "[−]";
      }
    });
  });
}

/**
 * Mobile navigation toggle
 */
function initMobileMenu() {
  const toggle = document.querySelector(".mobile-toggle");
  const menu = document.querySelector(".nav-menu");

  if (!toggle || !menu) return;

  toggle.addEventListener("click", () => {
    menu.classList.toggle("open");
  });

  menu.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => {
      menu.classList.remove("open");
    });
  });
}

/**
 * Mockup interactivity (fun responsive feedback for visitors)
 */
function initMockupInteractivity() {
  const joinBtn = document.getElementById("mockup-join-btn");
  const statusBtn = document.getElementById("mockup-status-btn");
  const countSpan = document.getElementById("mockup-joined-count");

  if (joinBtn && countSpan) {
    let joined = false;
    let baseCount = 14;

    joinBtn.addEventListener("click", () => {
      joined = !joined;
      if (joined) {
        countSpan.textContent = `${baseCount + 1} Coders`;
        joinBtn.textContent = "✓ Joined!";
        joinBtn.style.background = "#248046";
        showToast("Simulated: You joined today's challenge!");
      } else {
        countSpan.textContent = `${baseCount} Coders`;
        joinBtn.textContent = "Join Challenge";
        joinBtn.style.background = "";
      }
    });
  }

  if (statusBtn) {
    statusBtn.addEventListener("click", () => {
      showToast("Triggered: /challenge status command preview");
    });
  }
}

/**
 * Displays a brutalist toast notification
 */
function showToast(message) {
  let toast = document.querySelector(".toast");
  if (!toast) {
    toast = document.createElement("div");
    toast.className = "toast";
    document.body.appendChild(toast);
  }

  toast.innerHTML = `
    <span style="color: var(--accent-amber); font-weight: 800;">[OK]</span>
    <span>${message}</span>
  `;

  toast.classList.add("show");

  clearTimeout(window.toastTimer);
  window.toastTimer = setTimeout(() => {
    toast.classList.remove("show");
  }, 2200);
}

