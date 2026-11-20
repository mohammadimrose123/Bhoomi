import BrokerCTA from '../components/BrokerCTA'
import CategorySection from '../components/CategorySection'
import Hero from '../components/Hero'
import HowItWorks from '../components/HowItWorks'
import LandIntelligence from '../components/LandIntelligence'
import LandSearch from '../components/LandSearch'
import LocationSection from '../components/LocationSection'

function Home() {
  return (
    <>
      <Hero />
      <CategorySection />
      <LandSearch />
      <LocationSection />
      <LandIntelligence />
      <HowItWorks />
      <BrokerCTA />
    </>
  )
}

export default Home
