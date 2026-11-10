#!/usr/bin/env bash
set -e

KEYSTORE_NAME="my-upload-key.jks"
KEY_ALIAS="tasave-upload"

echo "=========================================================="
echo "          GENERADOR DE RELEASE AAB PARA PLAY STORE         "
echo "=========================================================="

if [ ! -f "$KEYSTORE_NAME" ]; then
    echo "⚠️  No se encontró $KEYSTORE_NAME. Vamos a generarlo:"
    keytool -genkey -v -keystore "$KEYSTORE_NAME" -alias "$KEY_ALIAS" \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass tasavepassword -keypass tasavepassword \
        -dname "CN=Julio, OU=TasaVE, O=Julcer26, L=Caracas, ST=Miranda, C=VE"
    echo "✅ Keystore $KEYSTORE_NAME creada exitosamente."
else
    echo "✅ Usando keystore existente: $KEYSTORE_NAME"
fi

echo "📦 Compilando App Bundle (.aab) firmado para producción..."
export KEYSTORE_PATH="$(pwd)/$KEYSTORE_NAME"
export KEYSTORE_PASSWORD="tasavepassword"
export KEY_ALIAS="$KEY_ALIAS"
export KEY_PASSWORD="tasavepassword"

./gradlew bundleRelease --no-daemon

echo ""
echo "🎉 ¡LISTO! Tu archivo para subir a Google Play Store está en:"
ls -lh app/build/outputs/bundle/release/app-release.aab
echo "=========================================================="
