// Dark Mode Theme Toggle
document.addEventListener('DOMContentLoaded', function() {
    const themeToggle = document.getElementById('theme-toggle');
    const themeIcon = document.getElementById('theme-icon');
    const htmlElement = document.documentElement;
    
    // Obtener tema actual (ya aplicado por el script inline en head)
    const currentTheme = htmlElement.getAttribute('data-theme') || 'light';
    
    // Sincronizar icono con el tema actual
    if (themeIcon) {
        themeIcon.textContent = currentTheme === 'dark' ? '☀️' : '🌙';
    }
    
    // Toggle theme con animación
    if (themeToggle) {
        themeToggle.addEventListener('click', function() {
            const currentTheme = htmlElement.getAttribute('data-theme');
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            
            // Agregar clase de animación
            themeToggle.classList.add('switching');
            
            // Cambiar tema
            htmlElement.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            
            // Actualizar icono
            themeIcon.textContent = newTheme === 'dark' ? '☀️' : '🌙';
            
            // Remover clase de animación
            setTimeout(() => {
                themeToggle.classList.remove('switching');
            }, 600);
        });
    }
    
    // Detectar cambios en preferencia del sistema
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        if (!localStorage.getItem('theme')) {
            const newTheme = e.matches ? 'dark' : 'light';
            htmlElement.setAttribute('data-theme', newTheme);
            if (themeIcon) {
                themeIcon.textContent = newTheme === 'dark' ? '☀️' : '🌙';
            }
        }
    });

    // Smooth scrolling for navigation links
    const links = document.querySelectorAll('a[href^="#"]');
    links.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');
            if (targetId === '#') return;
            
            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                targetElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Auto-hide alert messages after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s ease';
            alert.style.opacity = '0';
            setTimeout(() => {
                alert.remove();
            }, 500);
        }, 5000);
    });

    // Form validation enhancement
    const form = document.querySelector('.contact-form');
    if (form) {
        form.addEventListener('submit', function(e) {
            const button = form.querySelector('button[type="submit"]');
            if (button) {
                button.disabled = true;
                button.textContent = 'Enviando...';
                
                // Re-enable button after 3 seconds in case of error
                setTimeout(() => {
                    button.disabled = false;
                    button.textContent = 'Enviar Mensaje';
                }, 3000);
            }
        });

        // Real-time validation feedback
        const inputs = form.querySelectorAll('.form-input');
        inputs.forEach(input => {
            input.addEventListener('blur', function() {
                if (this.value.trim() === '' && this.hasAttribute('required')) {
                    this.classList.add('form-input-error');
                } else {
                    this.classList.remove('form-input-error');
                }
            });

            input.addEventListener('input', function() {
                if (this.classList.contains('form-input-error') && this.value.trim() !== '') {
                    this.classList.remove('form-input-error');
                }
            });
        });
    }

    // Add animation on scroll for principle cards
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry, index) => {
            if (entry.isIntersecting) {
                setTimeout(() => {
                    entry.target.style.opacity = '1';
                    entry.target.style.transform = 'translateY(0)';
                }, index * 100);
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    // Observe principle cards and benefit items
    const animatedElements = document.querySelectorAll('.principle-card, .benefit-item');
    animatedElements.forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(20px)';
        el.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
        observer.observe(el);
    });

    // Navbar scroll effect
    let lastScroll = 0;
    const navbar = document.querySelector('.navbar');
    
    window.addEventListener('scroll', () => {
        const currentScroll = window.pageYOffset;
        
        if (currentScroll > 100) {
            navbar.style.boxShadow = '0 4px 6px -1px rgba(0, 0, 0, 0.1)';
        } else {
            navbar.style.boxShadow = '0 1px 2px 0 rgba(0, 0, 0, 0.05)';
        }
        
        lastScroll = currentScroll;
    });

    // Add particle effect on hero section (optional enhancement)
    const hero = document.querySelector('.hero');
    if (hero) {
        // Create subtle animated background effect
        hero.style.position = 'relative';
        hero.style.overflow = 'hidden';
    }

    // Portfolio Filter Functionality
    const filterButtons = document.querySelectorAll('.filter-btn');
    const portfolioCards = document.querySelectorAll('.portfolio-card');

    if (filterButtons.length > 0 && portfolioCards.length > 0) {
        // Function to filter portfolio items
        function filterPortfolio(filterValue) {
            portfolioCards.forEach((card, index) => {
                const category = card.getAttribute('data-category');
                
                // Add fade out animation
                card.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
                card.style.opacity = '0';
                card.style.transform = 'scale(0.9)';
                
                setTimeout(() => {
                    if (filterValue === 'all' || category === filterValue) {
                        // Show the card
                        card.style.display = 'block';
                        
                        // Trigger reflow to ensure transition works
                        void card.offsetHeight;
                        
                        // Add fade in animation with staggered delay
                        setTimeout(() => {
                            card.style.opacity = '1';
                            card.style.transform = 'scale(1)';
                        }, index * 50);
                    } else {
                        // Hide the card
                        card.style.display = 'none';
                    }
                }, 300);
            });
        }

        // Add click event to filter buttons
        filterButtons.forEach(button => {
            button.addEventListener('click', function() {
                // Remove active class from all buttons
                filterButtons.forEach(btn => btn.classList.remove('active'));
                
                // Add active class to clicked button
                this.classList.add('active');
                
                // Get the filter value
                const filterValue = this.getAttribute('data-filter');
                
                // Filter portfolio cards
                filterPortfolio(filterValue);
            });
        });

        // Initialize cards with proper styling
        portfolioCards.forEach(card => {
            card.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        });

        // Add keyboard navigation
        filterButtons.forEach((button, index) => {
            button.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    this.click();
                } else if (e.key === 'ArrowRight') {
                    e.preventDefault();
                    const nextButton = filterButtons[index + 1] || filterButtons[0];
                    nextButton.focus();
                } else if (e.key === 'ArrowLeft') {
                    e.preventDefault();
                    const prevButton = filterButtons[index - 1] || filterButtons[filterButtons.length - 1];
                    prevButton.focus();
                }
            });
        });
    }

    // Principle Cards Popup Functionality
    const principleCards = document.querySelectorAll('.principle-card');
    
    // Create overlay for mobile
    const overlay = document.createElement('div');
    overlay.className = 'popup-overlay';
    overlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        z-index: 9998;
        opacity: 0;
        visibility: hidden;
        transition: all 0.3s ease;
    `;
    document.body.appendChild(overlay);
    
    if (principleCards.length > 0) {
        principleCards.forEach(card => {
            const popup = card.querySelector('.principle-popup');
            
            if (popup) {
                // For desktop: show on hover (handled by CSS)
                // For mobile/tablet: toggle on click
                card.addEventListener('click', function(e) {
                    // Check if we're on a touch device
                    if ('ontouchstart' in window || navigator.maxTouchPoints > 0) {
                        e.stopPropagation();
                        
                        // Close all other popups
                        principleCards.forEach(otherCard => {
                            if (otherCard !== card) {
                                const otherPopup = otherCard.querySelector('.principle-popup');
                                if (otherPopup) {
                                    otherPopup.style.opacity = '0';
                                    otherPopup.style.visibility = 'hidden';
                                }
                            }
                        });
                        
                        // Toggle current popup
                        const isVisible = popup.style.opacity === '1';
                        if (isVisible) {
                            popup.style.opacity = '0';
                            popup.style.visibility = 'hidden';
                            overlay.style.opacity = '0';
                            overlay.style.visibility = 'hidden';
                        } else {
                            popup.style.opacity = '1';
                            popup.style.visibility = 'visible';
                            popup.style.transform = 'translate(-50%, -50%)';
                            overlay.style.opacity = '1';
                            overlay.style.visibility = 'visible';
                        }
                    }
                });
                
                // Adjust popup position if it goes off-screen (desktop)
                card.addEventListener('mouseenter', function() {
                    if (!('ontouchstart' in window || navigator.maxTouchPoints > 0)) {
                        setTimeout(() => {
                            const rect = popup.getBoundingClientRect();
                            const viewportWidth = window.innerWidth;
                            
                            // Reset transform first
                            popup.style.left = '50%';
                            popup.style.right = 'auto';
                            popup.style.transform = 'translateX(-50%) translateY(0)';
                            
                            // Recalculate after reset
                            const newRect = popup.getBoundingClientRect();
                            
                            // Check if popup goes off right edge
                            if (newRect.right > viewportWidth - 20) {
                                popup.style.left = 'auto';
                                popup.style.right = '0';
                                popup.style.transform = 'translateY(0)';
                            }
                            // Check if popup goes off left edge
                            else if (newRect.left < 20) {
                                popup.style.left = '0';
                                popup.style.right = 'auto';
                                popup.style.transform = 'translateY(0)';
                            }
                        }, 50);
                    }
                });
            }
        });
        
        // Close popups when clicking overlay or outside on touch devices
        overlay.addEventListener('click', function() {
            principleCards.forEach(card => {
                const popup = card.querySelector('.principle-popup');
                if (popup) {
                    popup.style.opacity = '0';
                    popup.style.visibility = 'hidden';
                }
            });
            overlay.style.opacity = '0';
            overlay.style.visibility = 'hidden';
        });
        
        document.addEventListener('click', function(e) {
            if ('ontouchstart' in window || navigator.maxTouchPoints > 0) {
                if (!e.target.closest('.principle-card')) {
                    principleCards.forEach(card => {
                        const popup = card.querySelector('.principle-popup');
                        if (popup) {
                            popup.style.opacity = '0';
                            popup.style.visibility = 'hidden';
                        }
                    });
                    overlay.style.opacity = '0';
                    overlay.style.visibility = 'hidden';
                }
            }
        });
        
        // Add keyboard accessibility
        principleCards.forEach(card => {
            card.setAttribute('tabindex', '0');
            card.setAttribute('role', 'button');
            card.setAttribute('aria-expanded', 'false');
            
            card.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    const popup = this.querySelector('.principle-popup');
                    if (popup) {
                        const isVisible = popup.style.opacity === '1';
                        popup.style.opacity = isVisible ? '0' : '1';
                        popup.style.visibility = isVisible ? 'hidden' : 'visible';
                        if (!isVisible) {
                            popup.style.transform = 'translateX(-50%) translateY(0)';
                        }
                        this.setAttribute('aria-expanded', !isVisible);
                    }
                } else if (e.key === 'Escape') {
                    const popup = this.querySelector('.principle-popup');
                    if (popup) {
                        popup.style.opacity = '0';
                        popup.style.visibility = 'hidden';
                        overlay.style.opacity = '0';
                        overlay.style.visibility = 'hidden';
                        this.setAttribute('aria-expanded', 'false');
                    }
                }
            });
        });
    }
});
