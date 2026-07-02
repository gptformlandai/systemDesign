# Git GitHub Setup, Authentication, and Identity Gold Sheet

> Goal: make Git and GitHub setup boring, secure, and explainable: install, configure identity, choose SSH or HTTPS, handle credentials, sign commits, manage multiple accounts, and debug auth failures without panic.

---

## 1. Intuition

Git setup is your developer passport.

Before you collaborate, Git and GitHub need to know:

- who authored the commit
- how to authenticate you
- which remote account you are using
- where credentials are stored
- whether commits and tags are trusted

Bad setup creates confusing problems:

- commits show the wrong author
- pushes go to the wrong account
- SSH says permission denied
- HTTPS repeatedly asks for credentials
- enterprise SSO blocks access
- signed commits show as unverified

Senior mental model:

```text
identity != authentication != authorization != signing
```

---

## 2. Definition

- Definition: Git/GitHub setup is the configuration of local Git identity, remote authentication, credential storage, signing keys, and GitHub account access.
- Category: developer environment / collaboration security.
- Core idea: Git records authorship locally, while GitHub separately authenticates and authorizes access to hosted repositories.

---

## 3. Why It Exists

Git is distributed. You can create commits without GitHub.

That means setup has multiple layers:

| Layer | Example | Purpose |
|---|---|---|
| Git identity | `user.name`, `user.email` | commit author metadata |
| Remote URL | SSH or HTTPS URL | how Git reaches GitHub |
| Credential method | SSH key, token, credential helper | how Git proves who you are |
| Authorization | repo/team/org permission | what you can access |
| Signing | GPG, SSH, or S/MIME signature | integrity and identity verification |
| Enterprise identity | SAML/SSO, managed users | org-level access control |

Interview trap:

> Changing `user.email` does not fix GitHub authentication. It only changes commit metadata for future commits.

---

## 4. First-Time Setup

Check versions:

```bash
git --version
gh --version
```

Configure baseline identity:

```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
git config --global core.editor "code --wait"
```

Inspect where config came from:

```bash
git config --list --show-origin
git config --global --list
git config --local --list
```

Important config scopes:

| Scope | Command | Stored Where | Use |
|---|---|---|---|
| system | `--system` | machine-wide | rarely changed by app developers |
| global | `--global` | user account | default personal setup |
| local | `--local` | repo `.git/config` | repo-specific override |
| worktree | `--worktree` | linked worktree config | advanced multi-worktree cases |

---

## 5. SSH vs HTTPS

| Choice | How It Works | Best For | Watch Out |
|---|---|---|---|
| SSH | key pair proves identity | daily developer access | key loaded, right account, host config |
| HTTPS + token/helper | Git uses token via credential helper | locked-down networks, OAuth helpers | token scopes, SSO, credential cache |
| `gh auth` | GitHub CLI manages auth and can configure Git | terminal-first GitHub workflow | token scopes and active account |
| deploy key | SSH key tied to one repo | deployment/read-only automation | not ideal for broad automation |
| GitHub App token | app installation token | durable automation | app permissions and installation scope |

Simple recommendation:

- Use SSH for normal developer Git operations if your org allows it.
- Use Git Credential Manager or `gh auth setup-git` for HTTPS flows.
- Use GitHub Apps or OIDC-based automation for durable automation, not broad personal tokens.

---

## 6. SSH Setup Flow

Generate a key:

```bash
ssh-keygen -t ed25519 -C "you@example.com"
```

Start agent and add key:

```bash
ssh-add ~/.ssh/id_ed25519
```

Add public key to GitHub:

```bash
cat ~/.ssh/id_ed25519.pub
```

Test connection:

```bash
ssh -T git@github.com
```

Use SSH remote:

```bash
git remote set-url origin git@github.com:OWNER/REPO.git
git remote -v
```

Troubleshooting:

| Symptom | Check |
|---|---|
| `Permission denied (publickey)` | key added to GitHub, key loaded in agent, correct account |
| wrong GitHub username in SSH greeting | wrong key selected or host config issue |
| enterprise repo access denied | SSO authorization or team/repo permission |
| clone works but push fails | read-only permission, protected branch, fork workflow |

---

## 7. Multiple GitHub Accounts

Use SSH host aliases:

```text
Host github-personal
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519_personal
  IdentitiesOnly yes

Host github-work
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519_work
  IdentitiesOnly yes
```

Then remotes become:

```bash
git remote set-url origin git@github-work:ORG/REPO.git
git remote set-url origin git@github-personal:USER/REPO.git
```

Use per-repo identity:

```bash
git config user.name "Work Name"
git config user.email "work@example.com"
```

Or use conditional includes:

```text
[includeIf "gitdir:~/work/"]
    path = ~/.gitconfig-work

[includeIf "gitdir:~/personal/"]
    path = ~/.gitconfig-personal
```

Senior note:

> Multiple-account bugs are usually remote URL plus SSH key selection plus local commit email, not one single setting.

---

## 8. HTTPS And Credential Helpers

Git over HTTPS needs a credential strategy.

Check helpers:

```bash
git config --global credential.helper
git config --list --show-origin | grep credential
```

Common helpers:

- Git Credential Manager
- macOS keychain helper
- Windows Credential Manager
- Linux libsecret helper
- temporary memory cache

Avoid storing tokens in plain text unless you understand the risk.

GitHub CLI setup:

```bash
gh auth login
gh auth status
gh auth setup-git
```

HTTPS remote:

```bash
git remote set-url origin https://github.com/OWNER/REPO.git
```

Token rules:

- prefer fine-grained PATs over broad classic PATs when PATs are necessary
- set expiration dates
- authorize SSO when required
- store tokens only in approved helpers
- rotate tokens after exposure
- avoid using human PATs for long-lived automation

---

## 9. Commit Email And GitHub Verification

GitHub associates commits with accounts based on commit email and verified emails.

Check current identity:

```bash
git config user.name
git config user.email
```

Change latest local commit author:

```bash
git commit --amend --reset-author
```

Change older local commits:

```bash
git rebase -i origin/main
```

Caution:

- rewriting pushed commits changes commit hashes
- coordinate before changing shared history
- GitHub may still show old commits with old author data if already pushed and merged

---

## 10. Commit And Tag Signing

Signing options:

- GPG signing
- SSH signing
- S/MIME signing

Why sign:

- verify commit/tag integrity
- raise trust for release tags
- satisfy enterprise policy
- protect high-risk infrastructure repos

Common commands:

```bash
git config --global commit.gpgsign true
git config --global tag.gpgSign true
git commit -S -m "Add signed change"
git tag -s v1.2.3 -m "Release v1.2.3"
git log --show-signature -1
git verify-tag v1.2.3
```

SSH signing shape:

```bash
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
```

What signing proves:

- the commit/tag was signed by a key
- GitHub can verify the key is associated with an account

What signing does not prove:

- code is correct
- review happened
- tests passed
- the author was not compromised

---

## 11. Enterprise SSO And Managed Accounts

Enterprise GitHub may add:

- SAML SSO
- SCIM provisioning
- enterprise managed users
- IP allow lists
- required 2FA
- restricted personal tokens
- audit logs

Failure symptoms:

- token works for personal repo but not org repo
- `gh auth status` looks fine but API returns forbidden
- SSH authentication succeeds but repo access is denied
- user is in org but not in required team

Debug order:

1. Confirm remote URL.
2. Confirm active GitHub account.
3. Confirm repo permission.
4. Confirm SSO authorization.
5. Confirm token scopes.
6. Confirm branch/ruleset restrictions.

---

## 12. Safe Setup Checklist

For a new machine:

1. Install Git and GitHub CLI.
2. Set `user.name` and `user.email`.
3. Set default branch and editor.
4. Choose SSH or HTTPS.
5. Configure credential helper or SSH key.
6. Test clone/fetch/push on a sandbox repo.
7. Configure signing if required.
8. Verify `git remote -v`.
9. Verify `git config --list --show-origin`.
10. Document work vs personal account separation.

For an enterprise repo:

1. Check SSO.
2. Check team membership.
3. Check branch/ruleset policy.
4. Check CODEOWNERS.
5. Check workflow file protection.
6. Check token and SSH key audit expectations.

---

## 13. Failure Modes

### Wrong Author On Commits

Cause:

- global email is personal
- repo local email not set
- no conditional include

Fix:

```bash
git config user.email "work@example.com"
git commit --amend --reset-author
```

If already pushed:

- do not rewrite shared history casually
- decide whether attribution cleanup is worth coordination

### Push Uses Wrong GitHub Account

Cause:

- wrong remote host alias
- wrong SSH key selected
- `gh` authenticated as wrong account

Fix:

```bash
git remote -v
ssh -T git@github.com
gh auth status
```

### Token Works Locally But Fails In Org

Cause:

- missing SSO authorization
- insufficient scopes
- org blocks classic PATs
- repo permission missing

Fix:

- authorize token for org SSO
- use fine-grained PAT or GitHub App
- request correct team access

### Signed Commit Shows Unverified

Cause:

- key not uploaded
- email mismatch
- wrong signing format
- expired/revoked key

Fix:

```bash
git log --show-signature -1
git config --global --list | grep -E "sign|gpg"
```

---

## 14. Practical Question

> You joined a company and can clone a repo but cannot push. Your commits also show your personal email. How do you debug and fix this?

---

## 15. Strong Answer

I separate identity from access.

First, I check local commit identity:

```bash
git config user.name
git config user.email
git config --list --show-origin
```

Then I check remote/auth:

```bash
git remote -v
ssh -T git@github.com
gh auth status
```

If the remote is SSH, I confirm the correct key and GitHub account. If it is HTTPS, I check the credential helper and token. For the push failure, I verify repo permission, branch protection/rulesets, and SSO authorization. I set repo-local work email, amend only local commits if needed, and avoid rewriting pushed/shared history without coordination.

---

## 16. Revision Notes

- One-line summary: Git identity, GitHub authentication, authorization, and signing are separate layers.
- Three keywords: identity, credential, permission.
- One interview trap: thinking `user.email` fixes authentication.
- One memory trick: "Who wrote it? Who logged in? Who is allowed? Who signed it?"

---

## 17. Official Source Notes

- Git credentials docs: <https://git-scm.com/docs/gitcredentials>
- GitHub SSH docs: <https://docs.github.com/en/authentication/connecting-to-github-with-ssh>
- GitHub personal access token docs: <https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens>
- GitHub commit signature verification docs: <https://docs.github.com/en/authentication/managing-commit-signature-verification>
- GitHub signing commits docs: <https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits>
- GitHub CLI manual: <https://cli.github.com/manual/>
