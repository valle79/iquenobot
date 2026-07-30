DO $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN SELECT id FROM tenants WHERE is_deleted = FALSE LOOP

        -- =====================================================================
        -- AUTO-RESPUESTAS (INTENTS)
        -- =====================================================================

        -- 1. Saludo
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'saludo', 'Saludos cordiales de bienvenida',
               '["Hola","Buenos días","Buenas tardes","Buenas noches","Qué tal","Hey","Hola, cómo estás","Buen día"]'::jsonb::text,
               '["¡Hola! ¿En qué puedo ayudarte el día de hoy? Estoy aquí para servirte.","¡Buenos días! Bienvenido, ¿cómo puedo asistirte?","¡Hola! Un gusto saludarte. ¿En qué puedo servirte?"]'::jsonb::text,
               0.7, TRUE, 1, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'saludo');

        -- 2. Consulta de horario
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'consulta_horario', 'Consultas sobre horarios de atención',
               '["horario","horario de atención","a qué hora abren","a qué hora cierran","atienden sábados","atienden domingos","fines de semana","hasta qué hora atienden","días de atención"]'::jsonb::text,
               '["Nuestro horario de atención es de lunes a viernes de 9:00 a.m. a 6:00 p.m. y los sábados de 9:00 a.m. a 1:00 p.m. ¿En qué más puedo ayudarte?","Atendemos de lunes a viernes de 9am a 6pm, y sábados de 9am a 1pm. Domingo cerrado. ¿Algo más en que pueda servirte?"]'::jsonb::text,
               0.7, TRUE, 2, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'consulta_horario');

        -- 3. Solicitar precio
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'solicitar_precio', 'Consultas sobre precios y cotizaciones',
               '["precio","costo","cuánto vale","cuánto cuesta","tarifa","presupuesto","cotización","quiero comprar","me interesa"]'::jsonb::text,
               '["Gracias por tu interés. Permíteme consultar los precios actualizados para brindarte la mejor información. ¿Podrías indicarme qué producto o servicio te interesa?","Con gusto te ayudo con los precios. ¿Qué producto o servicio deseas consultar? Así puedo darte una cotización personalizada."]'::jsonb::text,
               0.7, TRUE, 3, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'solicitar_precio');

        -- 4. Ubicación
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'ubicacion', 'Consultas sobre dirección y ubicación',
               '["dirección","ubicación","dónde están","cómo llegar","mapa","oficina","local","dónde queda"]'::jsonb::text,
               '["Nos encontramos en [Dirección de la empresa]. ¿Deseas que te envíe la ubicación por Google Maps?","Puedes visitarnos en [Dirección de la empresa]. Estaremos encantados de atenderte en nuestro local."]'::jsonb::text,
               0.7, TRUE, 4, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'ubicacion');

        -- 5. Contactar asesor
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'contactar_asesor', 'Solicitud de contacto con un agente humano',
               '["agente","asesor","hablar con alguien","persona","operador","atención al cliente","ejecutivo","representante","transferir"]'::jsonb::text,
               '["Por supuesto, te conectamos con un asesor en este momento. Por favor, espera un instante.","Claro, un asesor se comunicará contigo a la brevedad. Gracias por tu paciencia.","Entiendo, en un momento te atenderá un agente. Por favor, mantente en línea."]'::jsonb::text,
               0.7, TRUE, 5, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'contactar_asesor');

        -- 6. Consulta de envío
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'consulta_envio', 'Consultas sobre envíos y entregas',
               '["envío","delivery","despacho","entrega","cuándo llega","tiempo de entrega","demora","seguimiento de pedido","rastrear"]'::jsonb::text,
               '["El tiempo de entrega estimado es de 2 a 5 días hábiles dentro de Lima y de 5 a 7 días hábiles para provincias. ¿Te gustaría rastrear tu pedido?","Realizamos envíos a todo el Perú. El costo y tiempo de entrega varían según tu ubicación. ¿Podrías indicarme tu distrito o ciudad para darte mayor precisión?"]'::jsonb::text,
               0.7, TRUE, 6, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'consulta_envio');

        -- 7. Forma de pago
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'forma_pago', 'Consultas sobre métodos de pago',
               '["pago","pagar","tarjeta","transferencia","yape","plin","efectivo","depósito","métodos de pago"]'::jsonb::text,
               '["Aceptamos pagos con tarjeta de crédito/débito, transferencia bancaria, Yape y Plin. ¿Cuál es tu método de pago preferido?","Puedes pagar con tarjeta (Visa, Mastercard), transferencia bancaria, Yape o Plin. ¿Cuál te resulta más conveniente?"]'::jsonb::text,
               0.7, TRUE, 7, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'forma_pago');

        -- 8. Queja o reclamo
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'queja_reclamo', 'Atención de quejas o reclamos',
               '["queja","reclamo","problema","insatisfecho","error","mal servicio","devolución","me quejo"]'::jsonb::text,
               '["Lamento mucho el inconveniente. Permíteme revisar tu caso para darte una solución rápida. ¿Podrías contarme un poco más sobre lo sucedido?","Agradecemos tu feedback, ya que nos ayuda a mejorar. Un asesor especializado revisará tu caso y te contactará a la brevedad.","Lamento el problema que has experimentado. Déjame tus datos y un supervisor se comunicará contigo en los próximos minutos para resolverlo."]'::jsonb::text,
               0.7, TRUE, 8, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'queja_reclamo');

        -- 9. Información de la empresa
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'informacion_empresa', 'Solicitud de información general de la empresa',
               '["quiénes son","empresa","acerca de","información","quién es","sobre ustedes","qué hacen"]'::jsonb::text,
               '["Somos una empresa comprometida con brindar el mejor servicio a nuestros clientes. ¿Hay algo específico que te gustaría saber sobre nosotros? Con gusto te informamos.","Con gusto te brindamos información sobre nuestra empresa. ¿Hay algo en particular que te gustaría conocer?"]'::jsonb::text,
               0.7, TRUE, 9, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'informacion_empresa');

        -- 10. Despedida
        INSERT INTO chatbot_intents (id, tenant_id, intent_name, description, training_phrases, responses, confidence_threshold, is_active, priority, matched_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'despedida', 'Despedidas cordiales',
               '["gracias","adiós","chau","hasta luego","nos vemos","que tengas buen día","muchas gracias","gracias por tu atención"]'::jsonb::text,
               '["Gracias a ti por contactarnos. ¡Que tengas un excelente día! Si necesitas algo más, aquí estamos.","Ha sido un placer atenderte. No dudes en escribirnos si requieres algo más. ¡Hasta pronto!","¡Gracias por comunicarte! Quedo atento por si surge cualquier otra consulta. Que tengas un maravilloso día."]'::jsonb::text,
               0.7, TRUE, 10, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_intents WHERE tenant_id = t.id AND intent_name = 'despedida');

        -- =====================================================================
        -- CONVERSACIONES GUIADAS (FLOWS)
        -- =====================================================================

        -- 1. Bienvenida
        INSERT INTO chatbot_flows (id, tenant_id, name, description, trigger_type, flow_config, is_active, priority, use_ai, success_count, failure_count, execution_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'Bienvenida', 'Saludo inicial cuando un cliente escribe por primera vez', 'WELCOME',
               '{"message":"¡Hola! Soy el asistente virtual de la empresa. Estoy aquí para ayudarte. Puedes consultarme sobre horarios, precios, productos, o si prefieres, puedo comunicarte con un asesor. ¿En qué puedo servirte el día de hoy?"}',
               TRUE, 1, FALSE, 0, 0, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_flows WHERE tenant_id = t.id AND name = 'Bienvenida' AND is_deleted = FALSE);

        -- 2. Consulta de precios
        INSERT INTO chatbot_flows (id, tenant_id, name, description, trigger_type, trigger_keywords, flow_config, fallback_message, is_active, priority, use_ai, success_count, failure_count, execution_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'Consulta de precios', 'Guía al cliente cuando pregunta por precios', 'KEYWORD',
               'precio, costo, cuánto vale, tarifa, cotización',
               '{"message":"Entiendo que deseas información sobre precios. Permíteme ayudarte con eso. ¿Podrías indicarme exactamente qué producto o servicio te interesa? Así puedo brindarte una cotización precisa y personalizada."}',
               'Gracias por tu consulta. Un asesor se comunicará contigo para darte los precios actualizados.',
               TRUE, 2, FALSE, 0, 0, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_flows WHERE tenant_id = t.id AND name = 'Consulta de precios' AND is_deleted = FALSE);

        -- 3. Soporte técnico
        INSERT INTO chatbot_flows (id, tenant_id, name, description, trigger_type, trigger_keywords, flow_config, fallback_message, is_active, priority, use_ai, success_count, failure_count, execution_count, created_at, updated_at, version, is_deleted)
        SELECT gen_random_uuid(), t.id, 'Soporte técnico', 'Deriva a soporte técnico cuando el cliente reporta un problema', 'KEYWORD',
               'soporte, ayuda técnica, problema técnico, falla, error, no funciona',
               '{"message":"Lamento que estés experimentando dificultades. Permíteme tomar nota de tu caso para que un especialista en soporte técnico te contacte a la mayor brevedad. Por favor, cuéntame brevemente cuál es el problema que estás presentando."}',
               'Gracias por reportarlo. Un técnico especializado revisará tu caso y te contactará pronto.',
               TRUE, 3, FALSE, 0, 0, 0, NOW(), NOW(), 0, FALSE
        WHERE NOT EXISTS (SELECT 1 FROM chatbot_flows WHERE tenant_id = t.id AND name = 'Soporte técnico' AND is_deleted = FALSE);

    END LOOP;
END $$;
