# ClosetAI Backend VPS Setup (systemd + nginx, IP-only, port 8081)

This deploys the backend at:

- API: `http://<VPS_IP>:8081/api/v1/...`
- Health: `http://<VPS_IP>:8081/health`
- Wardrobe uploads: `http://<VPS_IP>:8081/uploads/wardrobe/<file>`

## 0) Assumptions

- Ubuntu/Debian-like VPS
- You want public port **8081**
- You want uploads persisted at **`/var/lib/closetai/uploads`**
- Repo lives at `D:\ClosetAI` locally, but on the VPS we’ll put backend code in `/opt/closetai/backend`

## 1) Install OS packages

```bash
sudo apt update
sudo apt install -y python3 python3-venv python3-pip nginx git
```

## 2) Create directories and user (optional)

```bash
sudo useradd -r -s /usr/sbin/nologin closetai || true

sudo mkdir -p /opt/closetai/backend
sudo mkdir -p /var/lib/closetai/uploads
sudo mkdir -p /var/log/closetai

sudo chown -R $USER:$USER /opt/closetai
sudo chown -R closetai:closetai /var/lib/closetai /var/log/closetai
```

## 3) Copy backend code to VPS

Option A (git clone on VPS):

```bash
cd /opt/closetai
git clone <YOUR_REPO_URL> backend-repo
rsync -a --delete /opt/closetai/backend-repo/backend/ /opt/closetai/backend/
```

Option B (SCP just the backend folder):

```bash
scp -r ./backend ubuntu@<VPS_IP>:/opt/closetai/backend
```

## 4) Python venv + install deps

```bash
cd /opt/closetai/backend
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

## 5) Environment variables

Create `/etc/closetai/backend.env` (using your VPS IP `20.197.12.145`):

```bash
sudo tee /etc/closetai/backend.env > /dev/null <<'EOF'
# Persist uploads outside the code folder
UPLOADS_DIR=/var/lib/closetai/uploads

# Make returned image_url use your public address
PUBLIC_BASE_URL=http://20.197.12.145:8081

# If you use redis in production, set this too (example)
# REDIS_URL=redis://127.0.0.1:6379/0

# Try-on (optional)
# TRYON_SPACE_ID=...
# HF_TOKEN=...
EOF
```

Then secure it:

```bash
sudo chmod 600 /etc/closetai/backend.env
sudo chown root:root /etc/closetai/backend.env
```

## 6) systemd service

Copy the included unit file:

```bash
sudo cp /opt/closetai/backend/deploy/closetai-backend.service /etc/systemd/system/closetai-backend.service
sudo systemctl daemon-reload
sudo systemctl enable --now closetai-backend
sudo systemctl status closetai-backend --no-pager
```

Logs:

```bash
sudo journalctl -u closetai-backend -f
```

## 7) nginx reverse proxy on port 8081

Copy the included nginx site:

```bash
sudo cp /opt/closetai/backend/deploy/nginx-closetai-8081.conf /etc/nginx/sites-available/closetai-8081
sudo ln -sf /etc/nginx/sites-available/closetai-8081 /etc/nginx/sites-enabled/closetai-8081
sudo nginx -t
sudo systemctl restart nginx
```

## 8) Firewall / security group

Open inbound TCP **8081** in your VPS provider firewall / security group.

Quick check:

```bash
curl -v http://127.0.0.1:8081/health
curl -v http://20.197.12.145:8081/health
```

