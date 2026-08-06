import Navbar from './components/Navbar'
import Hero from './components/Hero'
import Marquee from './components/Marquee'
import Features from './components/Features'
import Showcase from './components/Showcase'
import HowItWorks from './components/HowItWorks'
import Tech from './components/Tech'
import Pricing from './components/Pricing'
import Testimonials from './components/Testimonials'
import FAQ from './components/FAQ'
import CTA from './components/CTA'
import Footer from './components/Footer'

export default function App() {
  return (
    <div className="noise relative min-h-screen overflow-hidden bg-void">
      <Navbar />
      <main>
        <Hero />
        <Marquee />
        <Features />
        <Showcase />
        <HowItWorks />
        <Tech />
        <Pricing />
        <Testimonials />
        <FAQ />
        <CTA />
      </main>
      <Footer />
    </div>
  )
}
