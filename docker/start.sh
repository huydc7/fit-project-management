#!/bin/sh
set -eu

# A CA certificate is public material; this store has no private keys.
# Keeping a separate JDBC truststore preserves public HTTPS trust for Supabase.
if [ -n "${DB_CA_CERT:-}" ]; then
  umask 077
  tls_dir=$(mktemp -d /tmp/quanlydetai-tls.XXXXXX)
  printf '%s\n' "$DB_CA_CERT" > "$tls_dir/aiven-ca.pem"
  keytool -importcert -noprompt -alias aiven-ca -file "$tls_dir/aiven-ca.pem" \
    -keystore "$tls_dir/truststore.p12" -storetype PKCS12 -storepass changeit >/dev/null
  export DB_TRUSTSTORE_URL="file:$tls_dir/truststore.p12"
  export DB_TRUSTSTORE_PASSWORD=changeit
fi
exec java -jar /app/app.jar
