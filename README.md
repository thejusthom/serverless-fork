# Serverless 

## Email Verification Service

## Overview

This serverless service verifies user email IDs using Google Cloud Functions. Upon new user registration, a verification email is sent with a unique link for email verification.

## Setup

1. **Prerequisites**: Ensure you have a GCP account with billing enabled, Google Cloud SDK installed, and necessary permissions to create resources.

3. **Deployment**: Deploy the Cloud Function using `terraform apply` after zipping the directory.

4. **Testing**: Test by creating a new user account and verifying the email.

## Usage

1. Register a new user account.
2. A verification email is sent automatically.
3. Click the verification link to confirm your email.