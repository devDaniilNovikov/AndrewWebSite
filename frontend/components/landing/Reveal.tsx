'use client';

import { motion, useReducedMotion } from 'motion/react';
import type { ReactNode } from 'react';

type RevealProps = Readonly<{
  children: ReactNode;
  className?: string;
  delay?: number;
}>;

export function Reveal({ children, className, delay = 0 }: RevealProps) {
  const shouldReduceMotion = useReducedMotion();

  if (shouldReduceMotion) {
    return <div className={className}>{children}</div>;
  }

  return (
    <motion.div
      className={className}
      data-reveal="motion"
      initial={{
        opacity: 'var(--reveal-initial-opacity)',
        y: 'var(--reveal-initial-offset)',
      }}
      transition={{ duration: 0.42, delay, ease: [0.22, 1, 0.36, 1] }}
      viewport={{ amount: 0.15, once: true }}
      whileInView={{ opacity: 1, y: 0 }}
    >
      {children}
    </motion.div>
  );
}
