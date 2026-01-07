#!/bin/bash

# Reactive Manifesto - Deployment Helper Script
# This script helps with various deployment tasks

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Reactive Manifesto - Deploy Helper      ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"
echo ""

# Function to display menu
show_menu() {
    echo -e "${GREEN}Selecciona una opción de deployment:${NC}"
    echo ""
    echo "  1) 🐳 Build Docker Image"
    echo "  2) 🚀 Run with Docker"
    echo "  3) 📦 Run with Docker Compose (with PostgreSQL)"
    echo "  4) 🎯 Deploy to Heroku"
    echo "  5) 🏗️  Build production package (SBT stage)"
    echo "  6) 🔑 Generate APPLICATION_SECRET"
    echo "  7) ✅ Test local deployment"
    echo "  8) 📋 Show deployment status"
    echo "  0) ❌ Exit"
    echo ""
}

# Generate secret
generate_secret() {
    echo -e "${YELLOW}Generando APPLICATION_SECRET...${NC}"
    if command -v openssl &> /dev/null; then
        SECRET=$(openssl rand -base64 32)
        echo -e "${GREEN}✓ Secret generado:${NC}"
        echo "$SECRET"
        echo ""
        echo -e "${YELLOW}Guarda este secret en un lugar seguro!${NC}"
        echo -e "${YELLOW}Configúralo como variable de entorno:${NC}"
        echo "  export APPLICATION_SECRET=\"$SECRET\""
    else
        echo -e "${RED}✗ openssl no está instalado${NC}"
    fi
}

# Build Docker image
build_docker() {
    echo -e "${YELLOW}Construyendo imagen Docker...${NC}"
    docker build -t reactive-manifesto:latest .
    echo -e "${GREEN}✓ Imagen construida exitosamente${NC}"
}

# Run with Docker
run_docker() {
    echo -e "${YELLOW}Ejecutando con Docker...${NC}"
    
    # Check if secret is provided
    if [ -z "$APPLICATION_SECRET" ]; then
        echo -e "${YELLOW}⚠ APPLICATION_SECRET no configurado, usando valor por defecto (inseguro)${NC}"
        SECRET="changeme-this-is-insecure"
    else
        SECRET="$APPLICATION_SECRET"
    fi
    
    docker run -d \
        --name reactive-manifesto \
        -p 9000:9000 \
        -e APPLICATION_SECRET="$SECRET" \
        reactive-manifesto:latest
    
    echo -e "${GREEN}✓ Contenedor iniciado${NC}"
    echo -e "Accede a la aplicación en: ${BLUE}http://localhost:9000${NC}"
}

# Run with Docker Compose
run_docker_compose() {
    echo -e "${YELLOW}Ejecutando con Docker Compose...${NC}"
    
    # Create .env if it doesn't exist
    if [ ! -f .env ]; then
        echo -e "${YELLOW}Creando archivo .env...${NC}"
        cat > .env << EOF
APPLICATION_SECRET=$(openssl rand -base64 32 2>/dev/null || echo "changeme")
DB_URL=jdbc:postgresql://db:5432/reactive_manifesto
DB_USER=postgres
DB_PASSWORD=postgres
EOF
        echo -e "${GREEN}✓ Archivo .env creado${NC}"
    fi
    
    docker-compose up -d
    echo -e "${GREEN}✓ Servicios iniciados${NC}"
    echo -e "Accede a la aplicación en: ${BLUE}http://localhost:9000${NC}"
    echo ""
    echo -e "Ver logs: ${YELLOW}docker-compose logs -f app${NC}"
}

# Deploy to Heroku
deploy_heroku() {
    echo -e "${YELLOW}Deploying to Heroku...${NC}"
    
    if ! command -v heroku &> /dev/null; then
        echo -e "${RED}✗ Heroku CLI no está instalado${NC}"
        echo "  Instala desde: https://devcenter.heroku.com/articles/heroku-cli"
        return 1
    fi
    
    echo "Ingresa el nombre de tu app en Heroku:"
    read APP_NAME
    
    if [ -z "$APP_NAME" ]; then
        echo -e "${RED}✗ Nombre de app requerido${NC}"
        return 1
    fi
    
    # Check if app exists
    if ! heroku apps:info -a "$APP_NAME" &> /dev/null; then
        echo -e "${YELLOW}App no existe. Creando nueva app...${NC}"
        heroku create "$APP_NAME"
    fi
    
    # Set environment variables
    echo -e "${YELLOW}Configurando variables de entorno...${NC}"
    heroku config:set APPLICATION_SECRET="$(openssl rand -base64 32)" -a "$APP_NAME"
    heroku config:set PLAY_ENV=prod -a "$APP_NAME"
    
    # Add PostgreSQL addon if desired
    echo "¿Deseas agregar PostgreSQL addon? (y/n)"
    read ADD_POSTGRES
    if [ "$ADD_POSTGRES" == "y" ]; then
        heroku addons:create heroku-postgresql:mini -a "$APP_NAME"
    fi
    
    # Deploy
    echo -e "${YELLOW}Deployando...${NC}"
    git push heroku main
    
    echo -e "${GREEN}✓ Deployment completado${NC}"
    heroku open -a "$APP_NAME"
}

# Build production package
build_stage() {
    echo -e "${YELLOW}Construyendo paquete de producción...${NC}"
    sbt clean compile stage
    echo -e "${GREEN}✓ Paquete construido en: target/universal/stage/${NC}"
    echo ""
    echo "Para ejecutar:"
    echo "  ./target/universal/stage/bin/web -Dhttp.port=9000"
}

# Test local deployment
test_deployment() {
    echo -e "${YELLOW}Testeando deployment local...${NC}"
    
    # Check if running
    if curl -s http://localhost:9000/ > /dev/null; then
        echo -e "${GREEN}✓ Aplicación respondiendo en http://localhost:9000${NC}"
        
        # Check status code
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/)
        echo -e "  Status code: ${BLUE}$STATUS${NC}"
        
        # Try API endpoint
        if curl -s http://localhost:9000/api/contacts > /dev/null; then
            echo -e "${GREEN}✓ API endpoint respondiendo${NC}"
        fi
    else
        echo -e "${RED}✗ No se pudo conectar a http://localhost:9000${NC}"
        echo "  Asegúrate de que la aplicación esté corriendo"
    fi
}

# Show deployment status
show_status() {
    echo -e "${BLUE}═══ Deployment Status ═══${NC}"
    echo ""
    
    # Docker
    echo -e "${YELLOW}Docker:${NC}"
    if docker ps | grep reactive-manifesto > /dev/null; then
        echo -e "  ${GREEN}✓ Contenedor corriendo${NC}"
        docker ps | grep reactive-manifesto
    else
        echo -e "  ${RED}✗ No hay contenedor corriendo${NC}"
    fi
    echo ""
    
    # Docker Compose
    echo -e "${YELLOW}Docker Compose:${NC}"
    if docker-compose ps 2>/dev/null | grep Up > /dev/null; then
        echo -e "  ${GREEN}✓ Servicios activos${NC}"
        docker-compose ps
    else
        echo -e "  ${RED}✗ No hay servicios activos${NC}"
    fi
    echo ""
    
    # Local SBT
    echo -e "${YELLOW}Local (SBT):${NC}"
    if lsof -i:9000 > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓ Servicio corriendo en puerto 9000${NC}"
        lsof -i:9000
    else
        echo -e "  ${RED}✗ No hay servicio en puerto 9000${NC}"
    fi
}

# Main menu loop
while true; do
    show_menu
    read -p "Opción: " choice
    echo ""
    
    case $choice in
        1) build_docker ;;
        2) run_docker ;;
        3) run_docker_compose ;;
        4) deploy_heroku ;;
        5) build_stage ;;
        6) generate_secret ;;
        7) test_deployment ;;
        8) show_status ;;
        0) 
            echo -e "${GREEN}¡Hasta luego!${NC}"
            exit 0 
            ;;
        *)
            echo -e "${RED}Opción inválida${NC}"
            ;;
    esac
    
    echo ""
    read -p "Presiona Enter para continuar..."
    clear
done
