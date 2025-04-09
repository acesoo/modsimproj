package rwsmodsimproj;

import javax.swing.Timer;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;

class Person {
    int x, y;
    boolean infected;
    boolean immune;
    boolean vulnerable;
    int age;
    int infectionTime;

    public Person(int x, int y, boolean infected, boolean vulnerable, int age) {
        this.x = x;
        this.y = y;
        this.infected = infected;
        this.immune = false;
        this.vulnerable = vulnerable;
        this.age = age;
        this.infectionTime = infected ? 0 : -1;
    }

    public void move(Random rand) {
        x += rand.nextInt(3) - 1; // Move -1, 0, or 1 in x
        y += rand.nextInt(3) - 1; // Move -1, 0, or 1 in y
    }
}

class SimulationPanel extends JPanel {
    List<Person> people;
    Random rand = new Random();

    // COVID-19 Inspired Parameters
    double baseInfectionRate = 0.40; // Probability of transmission in contact
    int recoveryMean = 400; // Mean recovery time in simulation ticks
    double reInfectionRate = 0.1; // Probability of re-infection for immune people
    double birthRate = 0.003; // Probability of birth per tick
    double deathRate = 0.001; // Probability of death per tick
    int maxPopulation = 1000; // Maximum cap for population

    // Statistics
    int totalPeople = 0;
    int totalInfected = 0;
    int totalRecovered = 0;
    int totalImmune = 0; // Counter for immune individuals
    int births = 0;
    int deaths = 0;

    public SimulationPanel(List<Person> people) {
        this.people = people;
        this.totalPeople = people.size();
    }

    public void updateSimulation() {
        // Update health states
        for (Person p : people) {
            p.move(rand);

            // Process recovery state
            if (p.infected) {
                p.infectionTime++;
                int recoveryTicks = calculateRecoveryTicks(p);
                if (p.infectionTime >= recoveryTicks) {
                    p.infected = false; // They are no longer infected
                    p.immune = true; // They now are immune
                    totalRecovered++;
                    totalImmune++; // Increment immune count
                    p.infectionTime = 0; // Reset infection timer to 0
                }
            }
        }

        // Handle infection spread
        for (Person p1 : people) {
            for (Person p2 : people) {
                if (p1 != p2 && p1.x == p2.x && p1.y == p2.y) { // If they meet
                    // Infecting mechanics
                    if (p1.infected && !p2.infected && !p2.immune) {
                        if (rand.nextDouble() < baseInfectionRate) {
                            p2.infected = true; // Infect p2
                            p2.infectionTime = 0; // Reset infection timer for new infection
                        }
                    }
                    if (p2.infected && !p1.infected && !p1.immune) {
                        if (rand.nextDouble() < baseInfectionRate) {
                            p1.infected = true; // Infect p1
                            p1.infectionTime = 0; // Reset infection timer for new infection
                        }
                    }
                    // Check re-infection for immune individuals
                    handleReinfection(p1);
                    handleReinfection(p2);
                }
            }
        }

        // Dynamic adjustments for birth and death rates
        if (totalPeople < maxPopulation) {
            int newBirths = (int) (birthRate * totalPeople);
            for (int i = 0; i < newBirths; i++) {
                people.add(new Person(rand.nextInt(100), rand.nextInt(100), false, false, 0));
            }
            births += newBirths;
        }

        int newDeaths = (int) (deathRate * totalPeople);
        for (int i = 0; i < newDeaths && !people.isEmpty(); i++) {
            people.remove(rand.nextInt(people.size()));
        }
        deaths += newDeaths;

        // Update counts for people
        totalPeople = people.size();
        totalInfected = (int) people.stream().filter(p -> p.infected).count();

        repaint(); // Update the view
    }

    private void handleReinfection(Person p) {
        if (p.immune && rand.nextDouble() < reInfectionRate) {
            p.infected = true; // Re-infect
            p.immune = false; // Lose immunity
            p.infectionTime = 0; // Reset infection timer for new infection
            totalImmune--; // Decrement immune count
        }
    }

    private int calculateRecoveryTicks(Person p) {
        int recoveryTicks = recoveryMean;

        // Dynamic recovery times based on age and comorbidities
        if (p.age < 18) {
            recoveryTicks = p.vulnerable ? 90 : 80; // Vulnerable youth vs. non-vulnerable
        } else if (p.age < 36) {
            recoveryTicks = p.vulnerable ? 110 : 100; // Vulnerable adults vs. non-vulnerable
        } else if (p.age < 56) {
            recoveryTicks = p.vulnerable ? 130 : 120; // Middle-aged
        } else {
            recoveryTicks = p.vulnerable ? 160 : 140; // Older adults
        }
        return recoveryTicks;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Person p : people) {
            if (p.infected) g.setColor(Color.RED);
            else if (p.immune) g.setColor(Color.GREEN);
            else g.setColor(Color.BLUE);
            g.fillOval(p.x * 5, p.y * 5, 5, 5);
        }

        // Display statistics
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Total People: " + totalPeople, 520, 20);
        g.drawString("Infected: " + totalInfected, 520, 40);
        g.drawString("Recovered: " + totalRecovered, 520, 60);
        g.drawString("Immune: " + totalImmune, 520, 80);
        g.drawString("Births: " + births, 520, 100);
        g.drawString("Deaths: " + deaths, 520, 120);
    }
}

public class RandomWalkVirusSimulation extends JFrame {
    private SimulationPanel panel;

    public RandomWalkVirusSimulation(List<Person> people) {
        panel = new SimulationPanel(people);
        add(panel);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        Timer timer = new Timer(100, e -> panel.updateSimulation());
        timer.start();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random rand = new Random();

        System.out.print("Enter number of people: ");
        int numPeople = scanner.nextInt();
        System.out.print("Enter number of initially infected people: ");
        int numInfected = scanner.nextInt();
        System.out.print("Enter percentage of people with comorbidities (0-100): ");
        int vulnerabilityPercentage = scanner.nextInt();
        System.out.print("Enter minimum age: ");
        int minAge = scanner.nextInt();
        System.out.print("Enter maximum age: ");
        int maxAge = scanner.nextInt();

        List<Person> people = new ArrayList<>();
        for (int i = 0; i < numPeople; i++) {
            boolean infected = i < numInfected;
            boolean vulnerable = rand.nextInt(100) < vulnerabilityPercentage;
            int age = rand.nextInt(maxAge - minAge + 1) + minAge;
            people.add(new Person(rand.nextInt(100), rand.nextInt(100), infected, vulnerable, age));
        }

        new RandomWalkVirusSimulation(people);
        scanner.close();
    }
}
