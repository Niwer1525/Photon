# Photon

Photon is the backend software for Niwer's stuff such as the engine/framework, official content-packs, Armo, Blog, official Discord bot, database for anti-cheat reports, crash-reports, accounts & much more.

## Run

The main entry point is `niwer.photon.PhotonEngine`. You can execute the jar itself.
But we recommand to use docker-compose with the suggested config down below :
```yml
services:
  photon:
    image: ghcr.io/<organization_name>/photon:latest
    container_name: photon-app
    restart: unless-stopped
    ports:
      - "127.0.0.1:7070:7070"
    volumes:
      - ./app_data:/photon_server/network/
```
If errors occur, check the logs but it may be due to JNA not being able to load the native library. To disable JNA unpacking and use a temporary directory for the native library:
```bash	
java -jar YOUR_JAR_FILE -Djna.nosys=true -Djna.nounpack=true -Djna.tmpdir=/tmp
```

## Configuration

Photon is configured via a JSON file : `network/config.json`, which contains settings for the network, database, Stripe integration and more.

- **General settings**
	- `isDevEnv` controls whether the server is running in development mode.

- **Discord Bot**
	- `bot_activity` controls the bot's activity status.
	- `discord_bot_token` is the bot token used to connect to Discord.
	- `discord_bot_id` is the bot's user ID, used for certain operations.
	- `official_discord_server_id` is the ID of the official Discord server, used for certain operations.
	- `network_console_channel_id` is the ID of the Discord channel where network console logs are sent.

- **Web Server**
	- `webserver_port` controls the port on which the web server listens.
	- `webserver_host` controls the host address for the web server.

- **Database backups**
	- `database_backup_enabled` controls whether the backup system runs at all.
	- `database_backup_on_startup` controls whether a backup is created during startup.
	- `database_backup_interval_minutes` controls how often recurring backups run.
	- `database_backup_directory` controls where backup files are written.
	- `database_backup_file_prefix` controls the generated backup filename prefix.

- **Licenses**
	- `products` is a list of products available when creating a license. Each entry contains an `id`, display `name`, optional `default_license_duration_days`, and Stripe `stripe_price_ids`. Photon retrieves Checkout Session line items from Stripe and matches their Price ID against `stripe_price_ids`.
	**Product**
		- `id` is the unique identifier for the product.
		- `name` is the display name for the product.
		- `discord_role_id` is the Discord role ID to assign to users who purchase this product.
		- `is_license_required` indicates if the user have to generate license keys to use the product.
		- `default_license_duration_days` is the default duration for licenses of this product (in days).
		- `stripe_price_ids` is a list of Stripe Price IDs associated with this product. Photon uses these IDs to match against Stripe Checkout Session line items.
		- `is_subscription` indicates if the product is a subscription (recurring) or a one-time purchase.
		- `repo_owner` is the GitHub repository owner for this product's assets.
		- `repo_name` is the GitHub repository name for this product's assets.
		- `excluded_release_tags` is a list of release tags to exclude when fetching assets from GitHub. Photon will ignore releases with these tags when listing available assets for this product.

- **Github**
	- `github_pat` is the personal access token used to access GitHub API for accessing private repositories and assets.
	- `github_template_repo` is the repository used as a template for new repositories created by Photon.

- **Mail**
	- `mail_smtp_host` is the SMTP server host for sending emails.
	- `mail_smtp_port` is the SMTP server port for sending emails.
	- `mail_smtp_username` is the username for authenticating with the SMTP server.
	- `mail_smtp_password` is the password for authenticating with the SMTP server.
	- `mail_sender_email` is the email address used as the sender for outgoing emails.

- **Stripe (Checkout & webhooks)**
	- `stripe_webhook_signature` the private signature key from Stripe dashboard (not the public API key. This is used to verify incoming webhook events from Stripe.
	- `stripe_api_key` enables Stripe checkout session creation.

- **Accounts**
	- `account_retention_days` controls how long deleted accounts are retained before permanent deletion.
	- `totp_issuer` is the issuer name used for TOTP (Time-based One-Time Password) generation.

## Endpoints

The routes below are the ones typically called by the game client, launcher, Stripe, or server processes. Admin-panel routes are kept in a separate section.

- **Accounts**
	- `POST /accounts/create_account` — Create an account.
		- Required fields: `username`, `email`, `password`.
		- Optional purchase flow fields: `token` or `checkoutSessionId`.
		- If no purchase token is provided, the email must already have an active subscription.
	- `POST /accounts/auth_account` — Log in and return `{ token, account }`.
		- Required fields: `email`, `password`.
	- `GET /accounts/me` — Return the current account.
		- Auth via `X-Photon-User-Token` or `Authorization: Bearer <token>`.
	- `GET /accounts/entitlements` — Return the account's subscriptions and one-time product purchases.
	- `POST /accounts/change_password` — Change a password.
		- Required body fields: `email`, `currentPassword`, `newPassword`.
	- `POST /accounts/update_profile` — Update username, email, or password.
		- Required body fields: `uuid`, `currentPassword`, optional `username`, `email`, `newPassword`, `confirmPassword`.
	- `GET /accounts/licenses` — List the caller’s licenses.
	- `POST /accounts/licenses` — Create a license for the current subscriber.
	- `POST /accounts/licenses/revoke` — Revoke one of the caller’s licenses.

- **Updates**
	- `GET /download/list` — List available download assets.
	- `GET /download` — Download a specific asset from GitHub.
		- Required query parameters: `assetId`, `product`, `assetName`.

- **Software (Games / Applications / Launchers)**
	- `GET /game/config` — Public runtime config used by the client.
	- `POST /game/add-crash-report` — Upload a crash report.
		- Required fields: `fileMessage`, `userUUID`, `timestamp`.
		- Optional field: `side`.
	- `POST /game/add-anticheat-report` — Upload an anti-cheat report.
		- Required fields: `fileMessage`, `userUUID`, `operatingSystem`, `timestamp`.
	- `POST /game/add-hwid` — Register a hardware ID.
		- Required fields: `hwid`, `userUUID`, `operatingSystem`.
	- `POST /licenses/validate` — Validate a license key on the client.
		- Required fields: `license_key`, `product_id`, `hardware_id`.

- **Servers**
	- `POST /servers/add-server` — Register or update a server entry.
		- The request IP must match the remote IP.
	- `GET /servers/server-list` — List known servers.
	- `GET /api/status/servers` — Status-oriented server listing used by the web UI.

- **Stripe**
	- `POST /stripe/webhook` — Stripe webhook endpoint.
		- Receives subscription events such as `customer.subscription.created`, `customer.subscription.updated`, `customer.subscription.deleted`.
		- Also handles `checkout.session.completed` for the purchase-token flow.

## Admin endpoints

These routes require an admin session or admin token.
- `GET /api/admin/tables`
- `GET /api/admin/tables/{table}`