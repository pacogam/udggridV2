import { css } from "lit";

export function getAppStyle() {
    return css`
      :host {
        --or-app-color1: #F9F5F2;
        --or-app-color2: #F9F5F2;
        --or-app-color3: #4F2D39;
        --or-app-color4: #4F2D39;
        --or-app-color5: #F9F5F2;
        --or-app-color6: #F44F1A;

        --og-color-primary: #F9F5F2;
        --og-color-secondary: #C6B9BA;
        --og-color-primary-dark: #4F2D39;
        --og-color-secondary-dark: #5F424C;
        --og-color-success: #00AA45;
        --og-color-success-light: #80d5a2;
        --og-color-neutral: #F2B344;
        --og-color-warning: #F26E44;
        --og-color-danger: #F44F1A;
        --og-color-danger-light: #faa78d;
        --og-color-error: rgb(239, 68, 68);

        --og-background-shade: rgba(79, 45, 57, 0.04);
        --og-background-tint: rgba(217, 217, 217, 0.04);
        --or-icon-fill: var(--og-color-primary-dark);
        
        --og-panel-border-radius: 20px;


        /* -------------------------------------------------------- */
        /*                      FONT SIZES                          */
        /* -------------------------------------------------------- */

        /* Titles */
        --og-font-size-title: 3.0rem; /* Main title */
        --og-font-size-heading1: 1.8rem; /* Main heading */
        --og-font-size-subheading1: 1.2rem; /* Subtitle Main heading; ... */
        --og-font-size-heading2: 1.2rem; /* Secondary heading; ... */
        --og-font-size-subheading2: 1.05rem; /* Subtitle Secondary heading; ... */

        /* Text */
        --og-font-size-primary: 1.2rem; /* Main text */
        --og-font-size-secondary: 1.05rem; /* Secondary text; ... */
        --og-font-size-tertiary: 0.9rem; /* Tertiary text; ... */

        /* Statistics */
        --og-font-size-statistic-large: 2.25rem;
        --og-font-size-statistic-medium: var(--og-font-size-heading2);

        /* Icons */
        --og-font-size-icon-large: var(--og-font-size-title);
        --og-font-size-icon-medium: var(--og-font-size-primary);
        --og-font-size-icon-small: var(--og-font-size-tertiary);

        /* Other */
        --og-font-size-menu-options: var(--og-font-size-secondary); /* Official spec; 16px */
        --og-font-size-button-text: var(--og-font-size-secondary); /* Official spec; 16px */
        --og-font-size-button-small: var(--og-font-size-tertiary); /* Official spec says 15px but we use 14px instead */


        /* -------------------------------------------------------- */
        /*                      FONT WEIGHTS                        */
        /* -------------------------------------------------------- */

        /* Titles */
        --og-font-weight-title: 900;
        --og-font-weight-heading1: 700;
        --og-font-weight-subheading1: 300;
        --og-font-weight-heading2: 700;
        --og-font-weight-subheading2: 300;

        /* Text */
        --og-font-weight-primary: 400;
        --og-font-weight-secondary: 300;
        --og-font-weight-tertiary: 300;

        /* Statistics */
        --og-font-weight-statistic-large: var(--og-font-weight-heading1);
        --og-font-weight-statistic-medium: var(--og-font-weight-heading2);

        /* Other */
        --og-font-weight-menu-options: var(--og-font-weight-primary);
        --og-font-weight-button-text: var(--og-font-weight-primary);
        --og-font-weight-button-small: var(--og-font-weight-primary);


        /* -------------------------------------------------------- */
        /*                      ANIMATIONS                          */
        /* -------------------------------------------------------- */

        --animate-offset: 0ms;

        /* Animation duration. Gathered from material spec: https://m2.material.io/design/motion/speed.html#duration */
        --og-duration-small: 100ms;
        --og-duration-medium-entry: 250ms;
        --og-duration-medium-exit: 200ms;
        --og-duration-large-entry: 300ms;
        --og-duration-large-exit: 300ms;

        /* Easing properties. Gathered from material spec: https://m2.material.io/design/motion/speed.html#easing */
        --og-easing-standard: cubic-bezier(0.4, 0.0, 0.2, 1);
        --og-easing-entry: cubic-bezier(0.0, 0.0, 0.2, 1);
        --og-easing-exit: cubic-bezier(0.4, 0.0, 1, 1);
      }


      /* -------------------------------------------------------- */
      /*             AVAILABLE FONT CLASSES TO USE                */
      /* -------------------------------------------------------- */

      /* Main title */

      .text-title {
        font-size: var(--og-font-size-title);
        font-weight: var(--og-font-weight-title);
        color: var(--og-color-primary-dark);
      }

      .text-title.dark {
        color: var(--og-color-primary);
      }

      /* Main heading */

      .text-heading {
        font-size: var(--og-font-size-heading1);
        font-weight: var(--og-font-weight-heading1);
        color: var(--og-color-primary-dark);
      }

      .text-heading.dark {
        color: var(--og-color-primary);
      }

      /* Subtitle of Main heading */

      .text-subheading {
        font-size: var(--og-font-size-subheading1);
        font-weight: var(--og-font-weight-subheading1);
        color: var(--og-color-primary-dark);
      }

      .text-subheading.dark {
        color: var(--og-color-primary);
      }

      /* Heading #2 */

      .text-heading2 {
        font-size: var(--og-font-size-heading2);
        font-weight: var(--og-font-weight-heading2);
        color: var(--og-color-primary-dark);
      }

      .text-heading2.dark {
        color: var(--og-color-primary);
      }

      /* Subtitle of Heading #2 */

      .text-subheading2 {
        font-size: var(--og-font-size-subheading2);
        font-weight: var(--og-font-weight-subheading2);
        color: var(--og-color-primary-dark);
      }

      .text-subheading2.dark {
        color: var(--og-color-primary);
      }

      /* ---------------- */

      /* Primary text */

      .text-primary {
        font-size: var(--og-font-size-primary);
        font-weight: var(--og-font-weight-primary);
      }
      .text-primary.bold {
        font-weight: var(--og-font-weight-heading1);
      }
      .text-primary.dark {
        color: var(--og-color-primary);
      }

      /* Secondary text */

      .text-secondary {
        font-size: var(--og-font-size-secondary);
        font-weight: var(--og-font-weight-secondary);
      }

      .text-secondary.bold {
        font-weight: var(--og-font-weight-primary);
      }

      .text-secondary.dark {
        color: var(--og-color-primary);
      }

      /* Tertiary text */

      .text-tertiary {
        font-size: var(--og-font-size-tertiary);
        font-weight: var(--og-font-weight-tertiary);
      }

      .text-tertiary.bold {
        font-weight: var(--og-font-weight-primary);
      }
      
      .text-tertiary.dark {
        color: var(--og-color-primary);
      }

      /* ------------- */

      .statistic-large {
        font-size: var(--og-font-size-statistic-large);
        font-weight: var(--og-font-weight-statistic-large);
        color: var(--og-color-primary-dark);
      }

      .statistic-large.dark {
        color: var(--og-color-primary);
      }

      .statistic-medium {
        font-size: var(--og-font-size-statistic-medium);
        font-weight: var(--og-font-weight-statistic-medium);
        color: var(--og-color-primary-dark);
      }

      .statistic-medium.dark {
        color: var(--og-color-primary);
      }
      
      .translucent {
        opacity: 0.4;
      }

      .background-tint {
        background-color: var(--og-background-tint);
      }

    `;
}