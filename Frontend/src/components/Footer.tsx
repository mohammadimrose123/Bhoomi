import { ArrowUpRight } from 'lucide-react'
import { Link } from 'react-router-dom'

function Footer() {
  return (
    <footer className="site-footer" id="contact">
      <div className="page-width">
        <div className="footer-main">
          <div className="footer-brand-block">
            <Link className="brand footer-brand" to="/">
              <span className="brand-mark" aria-hidden="true">B</span>
              <span className="brand-copy">
                <span className="brand-name">bhoomi<span>.</span></span>
                <span className="brand-caption">LAND, WITH CONTEXT</span>
              </span>
            </Link>
            <p>A more thoughtful way to discover land in Telangana. Built around better context, clearer information and confident next steps.</p>
            <span className="footer-location">Made for Telangana, India</span>
          </div>
          <div className="footer-links">
            <div><h3>Explore</h3><Link to="/explore">All land</Link><Link to="/#categories">Categories</Link><Link to="/#how-it-works">How it works</Link></div>
            <div><h3>For partners</h3><Link to="/register">List your land</Link><Link to="/register">Broker registration</Link><Link to="/login">Partner sign in</Link></div>
            <div><h3>About Bhoomi</h3><Link to="/#intelligence">Our approach</Link><a href="#contact">Contact updates</a><Link to="/privacy">Privacy policy</Link><Link to="/terms">Terms & conditions</Link></div>
          </div>
          <div className="footer-contact">
            <span className="footer-contact-label">Stay in the know</span>
            <p>Contact channels will be published when the platform is ready.</p>
            <div className="social-links" aria-label="Social channels coming soon">
              <span>Instagram · soon</span>
              <span>LinkedIn · soon</span>
            </div>
          </div>
        </div>
        <div className="footer-bottom">
          <span>© {new Date().getFullYear()} Bhoomi · Product preview</span>
          <span>Published inventory is broker-submitted and not independently verified.</span>
          <a href="#top" aria-label="Back to top">Back to top <ArrowUpRight size={14} /></a>
        </div>
      </div>
    </footer>
  )
}

export default Footer
