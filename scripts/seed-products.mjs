// Seed script: bulk-import products into the Productos module
// Usage: node scripts/seed-products.mjs
// You'll be prompted for your email and password to authenticate.

import { createInterface } from 'readline'
import { stdin as input, stdout as output } from 'process'

const API_BASE = process.env.API_URL || 'http://localhost:8085/api/v1'

function prompt(query) {
  const rl = createInterface({ input, output })
  return new Promise((resolve) => {
    rl.question(query, (answer) => {
      rl.close()
      resolve(answer)
    })
  })
}

async function api(url, options = {}) {
  const { headers: optHeaders, ...rest } = options
  const res = await fetch(`${API_BASE}${url}`, {
    ...rest,
    headers: { 'Content-Type': 'application/json', ...optHeaders },
  })
  const body = await res.json()
  if (!res.ok) throw new Error(`${res.status} ${body.message || res.statusText}\n${JSON.stringify(body, null, 2)}`)
  return body
}

// ── Product data ──────────────────────────────────────────────────────────────
const PRODUCTS = [
  {
    name: 'Desemplastificador Agrícola / Recolector de plástico y cinta',
    description: 'Equipo agrícola diseñado para la recolección eficiente de plástico y cinta de riego en los campos de cultivo. Incorpora un sistema hidráulico que facilita la operación, optimiza el tiempo de trabajo y contribuye al mantenimiento limpio y ordenado del terreno.\n\nEspecificaciones:\n• Material: acero reforzado\n\nCaracterísticas:\n• Sistema hidráulico que facilita el enrollado del material\n• Capacidad de recolección de plástico y cintas de riego de diferentes medidas\n• Estructura reforzada para trabajo en campo abierto\n• Enganche a tractor de fácil instalación\n• Diseño compacto y resistente para uso continuo\n• Reduce el tiempo de recolección manual\n• Contribuye a mantener el terreno limpio y preparado para el siguiente cultivo',
    shortDescription: 'Equipo agrícola diseñado para la recolección eficiente de plástico y cinta de riego en los campos de cultivo.',
    tags: 'desemplastificador, recoleccion, plastico, cinta de riego, agricola',
    weight: 0, width: 0, height: 0, length: 0,
  },
  {
    name: 'Subsolador de 3 Brazos',
    description: 'Subsolador de 3 Brazos. Equipo agrícola diseñado para romper y airear el suelo a mayor profundidad, mejorando la infiltración de agua y el desarrollo radicular. Sus tres brazos robustos permiten un trabajo eficiente en terrenos compactados, optimizando la preparación del terreno para siembras de alto rendimiento. Construido con materiales resistentes para mayor durabilidad y adaptado a distintas condiciones de cultivo.\n\nEspecificaciones:\n• Material: acero T-1\n• Potencia Requerida: 90 HP\n• Peso: 300 Kg\n\nCaracterísticas:\n• Implemento totalmente desmontable de fácil regulación\n• Castillo de enganche tres punto categoría II\n• Barra cuadrada solida acerada de 2 1/2" X 2.50 m\n• 3 Brazos curvos, acero T-1\n• Carteras con pernos de grado para los brazos\n• 3 Cajones (aletas) de tamaño estándar de fácil desmontar',
    shortDescription: 'Equipo agrícola diseñado para romper y airear el suelo a mayor profundidad.',
    tags: 'subsolador, brazos, suelo, agricola',
    weight: 300, width: 0, height: 0, length: 0,
  },
  {
    name: 'Cosechadora de Papa con Caja Importada',
    description: 'Nuestra cosechadora CP3FSI, diseñada para extracción eficiente de papas, combina robustez y rendimiento. Equipable con sistema de toma de fuerza (PTO), su diseño de sistema de cadenas asegura una cosecha rápida y limpia, reduciendo pérdidas de producto y facilitando el flujo continuo de trabajo. Ideal para trabajar en terrenos variados, su construcción resistente y mantenimiento simple la convierten en una herramienta imprescindible para tu campo.\n\nEspecificaciones:\n• Chasis metálico: acero estructural\n• Potencia Requerida: 70 HP\n• Nº de eslabones cadena delantera: 40 unidades\n• Nº de eslabones cadena posterior: 40 unidades\n• Peso Aprox.: 450 Kg\n\nCaracterísticas:\n• Enganche de tres puntos diseñada para todo tipo de terreno agrícola\n• Accesorios (Trompos, batidores, Rodillos y Piñones)\n• Cardan telescópico con protección (T4) IMPORTADO\n• Caja de transmisión IMPORTADO\n• Embrague IMPORTADO',
    shortDescription: 'Cosechadora CP3FSI diseñada para extracción eficiente de papas.',
    tags: 'cosechadora, papa, cosecha, agricola',
    weight: 450, width: 150, height: 120, length: 280,
  },
  {
    name: 'Cosechadora de Papa de Cola Larga CPFSI - 4',
    description: 'Máquina agrícola diseñada para extraer papas del suelo de forma eficiente con estructura extendida en la parte trasera que permite depositar las papas directamente en un remolque o carro mientras la máquina avanza, lo que mejora el rendimiento y reduce el daño a los tubérculos. Es ideal para trabajos en campos amplios y para operaciones que buscan optimizar el tiempo de cosecha.\n\nEspecificaciones:\n• Chasis metálico en plancha: Acero Estructural\n• Nº de eslabones cadena delantera: 60 unidades\n• Nº de eslabones cadena posterior: 50 unidades\n• Potencia Requerida: 70 Hp\n• Peso Aprox.: 480 Kg\n\nCaracterísticas:\n• Enganche de tres puntos diseñada para todo tipo de terreno agrícola\n• Cardan telescópico con protección (T4) MARCA AEMCO\n• Accesorios (Trompos, batidores, Rodillos y Piñones)\n• Ejes de transmisión en acero SAE 1045',
    shortDescription: 'Máquina agrícola para extraer papas con descarga directa a remolque.',
    tags: 'cosechadora, papa, cola larga, agricola',
    weight: 480, width: 150, height: 120, length: 280,
  },
  {
    name: 'Cosechadora de Papa con Descarga Lateral',
    description: 'La cosechadora de papa con descarga lateral de FSI Implementos Agrícolas está diseñada para maximizar la eficiencia en la recolección. Su sistema de descarga lateral permite un vaciado continuo y rápido, optimizando tiempos de trabajo. Construida con materiales resistentes y de alta durabilidad, garantiza un desempeño óptimo en terrenos exigentes. Ideal para pequeños y medianos agricultores que buscan rendimiento, ahorro y facilidad de operación. Respaldada con garantía, asesoría técnica y repuestos inmediatos en todo el Perú.\n\nEspecificaciones:\n• Material: acero estructural\n• N° eslabones cadena posterior: 50 unidades\n• N° eslabones cadena delantera: 60 unidades\n• Potencia requerida: 70 HP\n• Peso aprox.: 480 Kg\n\nCaracterísticas:\n• Enganche de tres puntos diseñada para todo tipo de terreno agrícola\n• Chasis metálico en plancha de acero estructural\n• Transmisión con 2 cajas de engranaje mediante corona y piñón, con sistema de embrague\n• Accesorios (Trompos, batidores, Rodillos y Piñones)\n• Cardan telescópico con protección (T4) MARCA AEMCO',
    shortDescription: 'Cosechadora con sistema de descarga lateral para vaciado continuo.',
    tags: 'cosechadora, papa, descarga lateral, agricola',
    weight: 480, width: 150, height: 120, length: 240,
  },
  {
    name: 'Picadora Estacionaria de Chala (10 HP)',
    description: 'Picadora ideal para cortar caña, pasto, malezas, y todo tipo de forrajes.\n\nEspecificaciones:\n• Chasis: acero estructural\n• Motor trifásico: 10 hp\n• Producción: 2 a 4 Tn/hr\n• Peso aproximado: 180 kg\n• N° de cuchillas: 8\n\nCaracterísticas:\n• Chasis en acero estructural\n• Tolva de alimentación manual y salida por ducto cuello de cisne\n• Poleas, fajas y chumacera de pie\n• Piñones de accionamiento y cadena de transmisión\n• Medidas de la máquina: 0.80 mt ancho x 2 mt largo x 1.80 mt alto\n• Repuestos disponibles: Cuchillas anti abrasivos y rodillos jaladores',
    shortDescription: 'Picadora para cortar caña, pasto, malezas y todo tipo de forrajes.',
    tags: 'picadora, chala, forraje, estacionaria',
    weight: 180, width: 80, height: 180, length: 200,
  },
  {
    name: 'Cultivadora de Brazos Rígidos',
    description: 'Barra cuadrada acerada.\n\nEspecificaciones:\n• Modelo: CULFSI 1\n• N° de brazos rectos: 6\n• Potencia requerida: 50 a 65 HP\n• Peso aproximado: 400 KG\n• N° de brazos curvos: 3\n\nCaracterísticas:\n• Barra cuadrada acerada\n• Equipo totalmente desmontable\n• Carteras con pernos oscilantes\n• Fácil regulación, distanciamiento y altura de los brazos\n• Castillo de enganche de tres puntos\n• Uñas desmontables y reversibles\n• Dimensiones de barra: 2 ½ x 2 ½ x 3 mt\n• Profundidad de trabajo: 25 cm\n• Repuestos disponibles: Brazo recto, Brazo curvo, Punta cincel, Punta V o flecha',
    shortDescription: 'Cultivadora de brazos rígidos para preparación de suelo.',
    tags: 'cultivadora, brazos, suelo, agricola',
    weight: 400, width: 300, height: 120, length: 150,
  },
  {
    name: 'Desbrozadora para Hoja de Papa con Martillos',
    description: 'Accionada por la tomafuerza del tractor, ideal para limpieza del terreno antes de la cosecha de papa/camote.\n\nEspecificaciones:\n• Chasis reforzado: acero estructural\n• Numero de martillos: 34\n• Ancho de trabajo: 1.80 mt\n• Potencia requerida: 80 a 90 HP\n• Peso aproximado: 650 Kg\n\nCaracterísticas:\n• Accionada por la tomafuerza del tractor\n• Chasis reforzado en acero estructural\n• Medidas de la máquina: 2.30 mt ancho x 2.50 mt largo x 1.10 mt alto\n• Repuestos disponibles: Martillos con corbata larga, Martillos con corbata corta\n• Enganche de tres puntos Categoria II',
    shortDescription: 'Desbrozadora accionada por tomafuerza para limpieza antes de cosecha.',
    tags: 'desbrozadora, papa, martillos, limpieza',
    weight: 650, width: 230, height: 110, length: 250,
  },
  {
    name: 'Cosechadora de Cebolla',
    description: 'Implemento especializado para la cosecha eficiente de cebollas.\n\nEspecificaciones:\n• Material: Chasis fabricado en acero estructural reforzado\n• Potencia requerida: 70 HP\n• Peso aproximado: 350 kg\n• Ancho de trabajo: 1.80 mt\n\nCaracterísticas:\n• Chasis fabricado en acero estructural reforzado\n• De enganche tres puntos, categoría II\n• Caja central de engranaje y piñones\n• Barra cuadrada en acero 1045\n• Cardán con protección de seguridad\n• Ruedas de corte con regulador de profundidad\n• Punta cincel en cada brazo\n• Ancho de trabajo: 1.80 mt\n• Dimensiones: 1.90 mt largo x 0.90 mt ancho x 1.30 mt alto\n• Repuestos disponibles: Rueda de corte, Barra cuadrada acerada',
    shortDescription: 'Implemento especializado para la cosecha eficiente de cebollas.',
    tags: 'cosechadora, cebolla, cosecha, agricola',
    weight: 350, width: 90, height: 130, length: 190,
  },
  {
    name: 'Abonadora con Sistema Hidráulico',
    description: 'Maquina totalmente desmontable, de fácil regulación, distanciamiento y altura de los brazos.\n\nEspecificaciones:\n• Material: Chasis en aceros estructural\n• Potencia requerida: 100 HP\n• Peso aproximado: 600 kg\n• Capacidad por tolva: 120 kg c/u\n\nCaracterísticas:\n• Chasis en aceros estructural\n• Accionamiento con motor hidráulico y válvula de control\n• Mangueras de alta presión hidráulicos\n• Castillo de enganche de tres puntos categoría II\n• 3 Tolvas de acero estructural para abono\n• Doble barras cuadradas de 2 1/2" x 3.20 m\n• 6 Brazos rectos acero\n• 3 Brazos curvos acero\n• 6 cajones y 3 puntas cincel\n• Carteras con perno de grado, regulables, para los brazos\n• Mangueras corrugadas adosable a los brazos rígidos\n• Repuestos disponibles: Brazos rectos, Brazos curvos, Puntas cincel',
    shortDescription: 'Abonadora hidráulica totalmente desmontable y de fácil regulación.',
    tags: 'abonadora, hidraulico, fertilizante, agricola',
    weight: 600, width: 320, height: 150, length: 150,
  },
  {
    name: 'Subsolador de 2 Brazos',
    description: 'Chasis tubular en perfil rectangular.\n\nEspecificaciones:\n• Potencia requerida: 80 a 90 HP\n• Peso aproximado: 330 kg\n• Profundidad de trabajo: 70 cm\n• Distancia entre brazos: 60 a 1.60 cm\n\nCaracterísticas:\n• Chasis tubular en perfil rectangular\n• Brazos curvos en acero anti-abrasivo\n• Puntas desmontables intercambiables\n• Fácil regulación\n• Repuestos disponibles: Cuchilla para subsolador, Ping de enganche, Brazo con cartera',
    shortDescription: 'Subsolador de 2 brazos para romper y airear el suelo.',
    tags: 'subsolador, 2 brazos, suelo, agricola',
    weight: 330, width: 160, height: 130, length: 70,
  },
  {
    name: 'Subsolador de 1 Brazo',
    description: 'Chasis tubular en perfil rectangular.\n\nEspecificaciones:\n• Potencia requerida: 50 a 60 HP\n• Peso aproximado: 150 kg\n• Profundidad de trabajo: 70 cm\n\nCaracterísticas:\n• Chasis tubular en perfil rectangular\n• Brazo curvo en acero anti-abrasivo\n• Puntas desmontables intercambiables\n• Fácil regulación\n• Repuestos disponibles: Cuchilla para subsolador, Ping de enganche, Brazo con cartera',
    shortDescription: 'Subsolador de 1 brazo para romper y airear el suelo.',
    tags: 'subsolador, 1 brazo, suelo, agricola',
    weight: 150, width: 60, height: 130, length: 70,
  },
  {
    name: 'Picadora Estacionaria de Chala (20 HP)',
    description: 'Picadora ideal para cortar caña, pasto, malezas, y todo tipo de forrajes.\n\nEspecificaciones:\n• Modelo: Chasis en acero estructural\n• Número de cuchillas: 3\n• Capacidad de producción: 3 a 5 TN / Hora\n• Peso aproximado: 650 kg\n• Motor trifásico: 20 hp\n\nCaracterísticas:\n• Chasis en acero estructural\n• Tolva de alimentación manual y salida por ducto cuello de cisne\n• Poleas, fajas y chumacera de pie\n• Piñones de accionamiento y cadena de transmisión\n• Caja accionada a motor eléctrico trifásico\n• Cuchillas anti abrasivos y rodillos jaladores',
    shortDescription: 'Picadora estacionaria de alta capacidad para forrajes.',
    tags: 'picadora, chala, forraje, 20hp',
    weight: 650, width: 120, height: 180, length: 200,
  },
  {
    name: 'Molino o Pulverizador de Cáscara de Coco',
    description: 'Trituradora y Molienda de coco seco.\n\nEspecificaciones:\n• Material: Chasis en acero estructural\n• Capacidad: 500 kg/h\n• Potencia: 10 HP (x2)\n• Sistema: Doble cajón\n\nCaracterísticas:\n• Chasis en acero estructural\n• Tolva de ingreso de 50 cm x 50 cm de coco seco\n• Ejes de trituración y pulverización\n• 48 martillos acerados para trituración\n• Zarandas con agujeros de Ø1" y Ø3/4"\n• Motor trifásico de 10 HP para trituración\n• 48 martillos acerados para pulverización\n• Zaranda con agujeros de Ø3 mm, 2 mm\n• Motor trifásico de 10 HP para pulverización\n• Ventilador de succión de polvillo (2 paletas)\n• Ciclón con caída para 2 salidas del polvillo de pulverización\n• 4 Poleas y fajas en "V"\n• Chumaceras de pared\n• Tablero para control de encendido y apagado',
    shortDescription: 'Trituradora y molienda de coco seco con doble sistema.',
    tags: 'molino, pulverizador, coco, trituradora',
    weight: 600, width: 200, height: 250, length: 150,
  },
  {
    name: 'Lampón Agrícola de Levante',
    description: 'Fabricado en plancha y perfiles de acero estructural.\n\nEspecificaciones:\n• Material: Acero estructural\n• Potencia requerida: 100 HP\n• Peso aproximado: 450 kg\n• Ancho de trabajo: 3.00 m\n\nCaracterísticas:\n• Fabricado en plancha y perfiles de acero estructural\n• Cuchilla en acero antidesgaste\n• Castillo de enganche 3 puntos\n• Ancho de trabajo: 3.00 m\n• Profundidad: 10 cm\n• Altura de trabajo: 50 cm',
    shortDescription: 'Lampón agrícola de levante fabricado en acero estructural.',
    tags: 'lampon, levante, acero, agricola',
    weight: 450, width: 300, height: 50, length: 10,
  },
  {
    name: 'Hoyadora Agrícola',
    description: 'Hoyadora de enganche de tres puntos, accionada con la toma de fuerza del tractor, chasis en acero tubular rectangular.\n\nEspecificaciones:\n• Chasis: acero tubular\n• Brocas: Ø6", Ø9", Ø12" y Ø20"\n• Peso: 180 kg\n• Potencia requerida: 70 HP\n• Broca: Ø6" a Ø8"\n\nCaracterísticas:\n• Caja reductora\n• Embrague para caja reductora\n• Cardán con protección accionado con la toma de fuerza del tractor\n• Barreno de perforación reforzado\n• Juego de cuchillas aceradas',
    shortDescription: 'Hoyadora de enganche de tres puntos para perforación de suelo.',
    tags: 'hoyadora, perforacion, suelo, agricola',
    weight: 180, width: 100, height: 170, length: 170,
  },
  {
    name: 'Encamadora Integral',
    description: 'Formador de cama, tira cinta de riego y coloca el plástico para el encamado, todo en un solo paso.\n\nEspecificaciones:\n• Material: Acero estructural y tubular\n• Potencia requerida: 100 HP\n• Peso aproximado: 600 kg\n\nCaracterísticas:\n• Formador de cama, tira cinta de riego y coloca el plástico para el encamado, todo en un solo paso\n• Chasis en acero estructural y tubular\n• Enganche tres puntos, categoría II\n• Formador de cama\n• Ancho de cama según necesidad\n• Diskillers, con sus respectivas carteras y discos\n• Rodillos portarollos de plástico\n• Rodillos alineadores para cinta de riego\n• Llantas lisas pisa plástico para sellado de la cama\n• Diskillers para tapar plástico\n• 2 vertederas',
    shortDescription: 'Formador de cama, tira cinta de riego y coloca plástico en un solo paso.',
    tags: 'encamadora, cama, riego, plastico',
    weight: 600, width: 180, height: 120, length: 160,
  },
  {
    name: 'Desgranadora de Maíz Duro',
    description: 'Desgranadora de maíz duro de alta eficiencia y durabilidad.\n\nEspecificaciones:\n• Material: Acero estructural\n• Potencia requerida: 70 HP\n• Peso: 350 kg\n• Producción: 14 a 18 Tn/hr\n\nCaracterísticas:\n• Material en acero estructural\n• Enganche tres puntos categoría II\n• Cardán accionada para toma de fuerza de tractor\n• Piñones y cadena para trasmisión del sistema\n• Tambor interior de desgrane\n• Tolva para ingreso de mazorcas\n• Ductos de salida de granos y coronta',
    shortDescription: 'Desgranadora de maíz duro de alta eficiencia y durabilidad.',
    tags: 'desgranadora, maiz, granos, agricola',
    weight: 350, width: 120, height: 150, length: 200,
  },
]

// ── Main ──────────────────────────────────────────────────────────────────────
async function main() {
  console.log('=== Seed: Importar Productos ===\n')

  const email = await prompt('Email: ')
  const password = await prompt('Contraseña: ')

  console.log('\nIniciando sesión...')
  const loginRes = await api('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  const token = loginRes.data.accessToken
  console.log('Sesión iniciada correctamente\n')

  const authHeaders = { Authorization: `Bearer ${token}` }

  let created = 0
  let errors = 0

  for (const p of PRODUCTS) {
    const body = {
      name: p.name,
      description: p.description,
      shortDescription: p.shortDescription,
      price: 0,
      stockQuantity: 0,
      lowStockThreshold: 5,
      status: 'ACTIVE',
      tags: p.tags,
      weight: p.weight,
      width: p.width,
      height: p.height,
      length: p.length,
    }

    process.stdout.write(`  ${p.name}... `)

    try {
      const res = await api('/products', {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify(body),
      })
      console.log(`✓ (ID: ${res.data.id.slice(0, 8)}…)`)
      created++
    } catch (err) {
      console.log(`✗ ${err.message}`)
      errors++
    }
  }

  console.log(`\nResumen: ${created} creados, ${errors} errores`)
}

main().catch(console.error)
