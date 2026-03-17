-- Provide gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'CLIENT' ou 'NUTRITIONIST'
    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabela de clientes, associada a usuários do tipo CLIENT (clients referenced by other tables)
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    name VARCHAR(255),
    birth_date DATE,
    height NUMERIC(5,2),
    weight NUMERIC(5,2),

    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabela de nutricionistas, associada a usuários do tipo NUTRITIONIST
CREATE TABLE nutritionists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    name VARCHAR(255) NOT NULL,
    description TEXT,
    profile_photo TEXT,
    banner_image TEXT,
    crn VARCHAR(50),
    consultation_price NUMERIC(10,2),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE nutritionist_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nutritionist_id UUID REFERENCES nutritionists(id) ON DELETE CASCADE,

    day_of_week INTEGER, -- 0 = domingo, 6 = sábado
    start_time TIME,
    end_time TIME
);

CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nutritionist_id UUID REFERENCES nutritionists(id) ON DELETE CASCADE,

    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    duration_days INTEGER,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabela para armazenar registros de clientes, como notas, planos de dieta, etc.
CREATE TABLE client_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID REFERENCES clients(id) ON DELETE CASCADE,
    nutritionist_id UUID REFERENCES nutritionists(id) ON DELETE CASCADE,

    notes TEXT,
    diet_plan TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabela de assinaturas, associando clientes a planos de nutricionistas
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID REFERENCES clients(id) ON DELETE CASCADE,
    plan_id UUID REFERENCES plans(id) ON DELETE CASCADE,

    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);
