package com.backend.nmcomputercare.newsletter.mailing;

/**
 * Builds the HTML body for a newsletter notification email sent to subscribers.
 *
 * <p>Uses inline CSS so the layout renders correctly in Gmail, Outlook, and
 * Apple Mail without requiring an external stylesheet.  Call
 * {@link #build(String, String, String, String)} with the subscriber's
 * name and newsletter details to obtain a ready-to-send HTML string.
 */
public final class NewsletterEmailTemplate {

    private NewsletterEmailTemplate() {}

    // Category pill colours (background : text).
    private static final String COLOUR_PROMO       = "#FF6B35";
    private static final String COLOUR_MAINTENANCE = "#4A90D9";
    private static final String COLOUR_GENERAL     = "#27AE60";
    private static final String COLOUR_UPDATE      = "#8E44AD";
    private static final String COLOUR_DEFAULT     = "#555555";

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * @param subscriberName  recipient's display name (e.g. "Kelly")
     * @param title           newsletter headline
     * @param contentPreview  first ~300 chars of the newsletter body
     * @param unsubscribeLink URL the recipient can visit to opt out
     * @return complete HTML document string ready to be sent as a MIME message
     */
    public static String build(
            String subscriberName,
            String title,
            String contentPreview,
            String unsubscribeLink) {


        String previewText  = truncate(stripHtml(contentPreview), 300);
        String currentYear  = String.valueOf(java.time.Year.now().getValue());

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
                  <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
                  <title>%s</title>
                  <!--[if mso]>
                  <noscript><xml><o:OfficeDocumentSettings>
                    <o:PixelsPerInch>96</o:PixelsPerInch>
                  </o:OfficeDocumentSettings></xml></noscript>
                  <![endif]-->
                </head>
                <body style="margin:0;padding:0;background-color:#f0f4f8;font-family:Arial,Helvetica,sans-serif;">
                
                  <!-- Preheader (hidden, visible in inbox preview) -->
                  <div style="display:none;font-size:1px;color:#f0f4f8;line-height:1px;
                              max-height:0;max-width:0;opacity:0;overflow:hidden;">
                    %s — NM Computer Care Newsletter
                  </div>
                
                  <!-- Outer wrapper -->
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                         style="background-color:#f0f4f8;">
                    <tr>
                      <td align="center" style="padding:32px 16px;">
                
                        <!-- Email card -->
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0"
                               style="max-width:600px;width:100%%;background:#ffffff;
                                      border-radius:12px;overflow:hidden;
                                      box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                
                          <!-- ── Header banner ───────────────────────────────── -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#1a1a2e 0%%,#16213e 50%%,#0f3460 100%%);
                                       padding:40px 40px 32px;text-align:center;">
                
                              <!-- Logo mark -->
                              <div style="display:inline-block;background:#e94560;border-radius:10px;
                                          padding:10px 18px;margin-bottom:20px;">
                                <span style="color:#ffffff;font-size:20px;font-weight:700;
                                             letter-spacing:1px;">NM</span>
                              </div>
                
                              <h1 style="color:#ffffff;font-size:15px;font-weight:400;
                                         letter-spacing:3px;text-transform:uppercase;
                                         margin:0 0 16px;">Computer Care</h1>
                
                              <!-- Category pill -->
                              <span style="display:inline-block;background:%s;color:#ffffff;
                                           font-size:11px;font-weight:700;letter-spacing:1.5px;
                                           text-transform:uppercase;padding:5px 14px;
                                           border-radius:20px;">%s</span>
                            </td>
                          </tr>
                
                          <!-- ── Greeting ────────────────────────────────────── -->
                          <tr>
                            <td style="padding:40px 40px 0;">
                              <p style="margin:0 0 8px;color:#555;font-size:14px;">Hello, <strong>%s</strong> 👋</p>
                              <p style="margin:0;color:#888;font-size:13px;">
                                We have something new for you from NM Computer Care.
                              </p>
                            </td>
                          </tr>
                
                          <!-- ── Newsletter title ────────────────────────────── -->
                          <tr>
                            <td style="padding:24px 40px 0;">
                              <h2 style="margin:0;color:#1a1a2e;font-size:26px;font-weight:700;
                                         line-height:1.3;">%s</h2>
                              <div style="width:48px;height:4px;background:#e94560;
                                          border-radius:2px;margin:12px 0 0;"></div>
                            </td>
                          </tr>
                
                          <!-- ── Content preview ────────────────────────────── -->
                          <tr>
                            <td style="padding:24px 40px 0;">
                              <div style="background:#f8fafc;border-left:4px solid #e94560;
                                          border-radius:0 8px 8px 0;padding:20px 24px;">
                                <p style="margin:0;color:#444;font-size:15px;line-height:1.7;">
                                  %s&hellip;
                                </p>
                              </div>
                            </td>
                          </tr>
                
                          <!-- ── Divider ─────────────────────────────────────── -->
                          <tr>
                            <td style="padding:32px 40px 0;">
                              <hr style="border:none;border-top:1px solid #eef0f3;margin:0;"/>
                            </td>
                          </tr>
                
                          <!-- ── Info grid ──────────────────────────────────── -->
                          <tr>
                            <td style="padding:24px 40px 0;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td style="width:50%%;padding-right:8px;">
                                    <div style="background:#f8fafc;border-radius:8px;padding:16px;">
                                      <p style="margin:0 0 4px;color:#aaa;font-size:11px;
                                                 text-transform:uppercase;letter-spacing:1px;">Category</p>
                                      <p style="margin:0;color:#1a1a2e;font-size:14px;font-weight:600;">%s</p>
                                    </div>
                                  </td>
                                  <td style="width:50%%;padding-left:8px;">
                                    <div style="background:#f8fafc;border-radius:8px;padding:16px;">
                                      <p style="margin:0 0 4px;color:#aaa;font-size:11px;
                                                 text-transform:uppercase;letter-spacing:1px;">From</p>
                                      <p style="margin:0;color:#1a1a2e;font-size:14px;font-weight:600;">
                                        NM Computer Care
                                      </p>
                                    </div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                
                          <!-- ── Why you received this ──────────────────────── -->
                          <tr>
                            <td style="padding:32px 40px 0;">
                              <div style="background:#fff8e1;border-radius:8px;padding:16px 20px;
                                          border:1px solid #ffe082;">
                                <p style="margin:0;color:#7a6000;font-size:13px;line-height:1.6;">
                                  <strong>Why this email?</strong> You subscribed to NM Computer Care
                                  newsletters. We send updates on maintenance tips, promotions, and
                                  tech news relevant to your devices.
                                </p>
                              </div>
                            </td>
                          </tr>
                
                          <!-- ── Footer ─────────────────────────────────────── -->
                          <tr>
                            <td style="padding:32px 40px 40px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td style="text-align:center;">
                                    <p style="margin:0 0 8px;color:#aaa;font-size:12px;">
                                      &copy; %s NM Computer Care. All rights reserved.
                                    </p>
                                    <p style="margin:0;font-size:12px;color:#aaa;">
                                      Don't want these emails?
                                      <a href="%s"
                                         style="color:#e94560;text-decoration:none;font-weight:600;">
                                        Unsubscribe
                                      </a>
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                
                        </table>
                        <!-- /email card -->
                
                      </td>
                    </tr>
                  </table>
                
                </body>
                </html>
                """
                .formatted(
                        title,           // <title>
                        title,           // preheader
                        subscriberName,  // greeting name
                        title,           // newsletter headline
                        previewText,     // content preview
                        currentYear,     // footer year
                        unsubscribeLink  // footer unsubscribe href
                );
    }

    // ──────────────────────────────────────────────────────────────────────────

    private static String pillColour(String category) {
        if (category == null) return COLOUR_DEFAULT;
        return switch (category.toUpperCase()) {
            case "PROMO"       -> COLOUR_PROMO;
            case "MAINTENANCE" -> COLOUR_MAINTENANCE;
            case "GENERAL"     -> COLOUR_GENERAL;
            case "UPDATE"      -> COLOUR_UPDATE;
            default            -> COLOUR_DEFAULT;
        };
    }

    /** Strip basic HTML tags so raw HTML content doesn't leak into plain preview. */
    private static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s{2,}", " ").trim();
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text == null ? "" : text;
        return text.substring(0, max).trim();
    }
}